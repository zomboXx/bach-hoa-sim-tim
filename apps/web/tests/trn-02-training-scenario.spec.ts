import { test, expect } from "@playwright/test";
import {
  evaluateTrainingSession,
  evaluateLegacyBrowserSimulation,
  deriveCompletedStages,
  resolveActionRight,
  TrainingCatalogValidationError,
  REQUIRED_STAGES,
  SCENARIO_CATALOG_V1,
  type TrainingActionRecord,
  type TrainingStage,
} from "../src/training/scenario";

const TRAINING_ROUTE = process.env.TRN_ROUTE || "/training";

test.describe("TRN-02 Kịch bản đào tạo PWA & Oracle đánh giá", () => {
  test("Oracle đánh giá thuần nhất tính điểm và xếp loại chính xác (Độc lập Client/Server)", () => {
    const requiredStages: TrainingStage[] = [
      "greet",
      "milk",
      "checkout",
      "cooler",
      "quarantine",
      "mentor",
    ];

    // Ca 1: Hoàn hảo không lỗi (0 lỗi -> 100 điểm, Xuất sắc)
    const perfectActions: TrainingActionRecord[] = requiredStages.map((st, i) => ({
      sequence: i + 1,
      stage: st,
      actionType: st,
      choiceText: "Lựa chọn đúng",
      isRight: true,
      feedback: "Tốt lắm",
      timestamp: 1000 + i,
    }));
    const resPerfect = evaluateLegacyBrowserSimulation(perfectActions, requiredStages);
    expect(resPerfect.score).toBe(100);
    expect(resPerfect.errors).toBe(0);
    expect(resPerfect.passed).toBe(true);
    expect(resPerfect.grade).toBe("EXCELLENT");
    expect(resPerfect.gradeLabel).toContain("Xuất sắc");

    // Ca 2: Có 2 lần sai được hướng dẫn lại (2 lỗi -> 80 điểm, Đạt yêu cầu)
    const twoErrorActions: TrainingActionRecord[] = [
      ...perfectActions,
      {
        sequence: 10,
        stage: "greet",
        actionType: "greet_customer",
        choiceText: "Sai 1",
        isRight: false,
        feedback: "Nhắc nhở 1",
        timestamp: 2000,
      },
      {
        sequence: 11,
        stage: "cooler",
        actionType: "inspect_cooler",
        choiceText: "Sai 2",
        isRight: false,
        feedback: "Nhắc nhở 2",
        timestamp: 2001,
      },
    ];
    const resTwoErrors = evaluateLegacyBrowserSimulation(twoErrorActions, requiredStages);
    expect(resTwoErrors.score).toBe(80);
    expect(resTwoErrors.errors).toBe(2);
    expect(resTwoErrors.passed).toBe(true);
    expect(resTwoErrors.grade).toBe("SATISFACTORY");
    expect(resTwoErrors.gradeLabel).toContain("Đạt yêu cầu");

    // Ca 3: Có 5 lần sai (5 lỗi -> 50 điểm, Cần luyện tập thêm)
    const fiveErrorActions: TrainingActionRecord[] = [
      ...perfectActions,
      ...Array.from({ length: 5 }, (_, idx) => ({
        sequence: 20 + idx,
        stage: "milk" as TrainingStage,
        actionType: "pick_product",
        choiceText: `Sai ${idx}`,
        isRight: false,
        feedback: "Nhắc nhở",
        timestamp: 3000 + idx,
      })),
    ];
    const resFiveErrors = evaluateLegacyBrowserSimulation(fiveErrorActions, requiredStages);
    expect(resFiveErrors.score).toBe(50);
    expect(resFiveErrors.errors).toBe(5);
    expect(resFiveErrors.grade).toBe("NEEDS_PRACTICE");
    expect(resFiveErrors.gradeLabel).toContain("Cần luyện tập thêm");

    // Ca 4: Không âm điểm khi lỗi >= 11 (12 lỗi -> max(0, 100 - 120) = 0 điểm)
    const twelveErrorActions: TrainingActionRecord[] = Array.from({ length: 12 }, (_, idx) => ({
      sequence: idx + 1,
      stage: "milk" as TrainingStage,
      actionType: "pick_product",
      choiceText: `Sai ${idx}`,
      isRight: false,
      feedback: "Nhắc nhở",
      timestamp: 4000 + idx,
    }));
    const resZero = evaluateLegacyBrowserSimulation(twelveErrorActions, requiredStages);
    expect(resZero.score).toBe(0);
    expect(resZero.errors).toBe(12);

    // Ca 5: Bỏ dở hoặc chưa đủ 6 bước -> passed = false
    const partialStages: TrainingStage[] = ["greet", "milk"];
    const resPartial = evaluateLegacyBrowserSimulation(perfectActions, partialStages);
    expect(resPartial.passed).toBe(false);
    expect(resPartial.completedSteps).toBe(2);

    // Ca 6: Suy diễn hoàn thành các bước từ chuỗi sự kiện định danh ổn định (Không truyền completedStages)
    const stableActions: TrainingActionRecord[] = [
      {
        sequence: 1,
        stage: "greet",
        actionType: "greet_customer",
        targetKey: "customer_linh",
        choiceKey: "greet_confirm_need",
        choiceText: "Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ? Em lấy giúp chị nhé.",
        isRight: true,
        feedback: "Linh: Đúng rồi",
        timestamp: 1001,
      },
      {
        sequence: 2,
        stage: "milk",
        actionType: "pick_product",
        targetKey: "shelf_milk",
        choiceKey: "milk_pick_2_low_sugar",
        choiceText: "Lấy 2 hộp sữa ít đường · 8.500đ / hộp",
        isRight: true,
        feedback: "Tốt lắm",
        timestamp: 1002,
      },
      {
        sequence: 3,
        stage: "checkout",
        actionType: "scan_item",
        targetKey: "pos_scanner",
        choiceKey: "scan_item_1",
        choiceText: "Quét hộp sữa 1",
        isRight: true,
        feedback: "Đã quét",
        timestamp: 1003,
      },
      {
        sequence: 4,
        stage: "checkout",
        actionType: "scan_item",
        targetKey: "pos_scanner",
        choiceKey: "scan_item_2",
        choiceText: "Quét hộp sữa 2",
        isRight: true,
        feedback: "Đã quét",
        timestamp: 1004,
      },
      {
        sequence: 5,
        stage: "checkout",
        actionType: "tender_payment",
        targetKey: "pos_cash",
        choiceKey: "tender_3k_receipt",
        choiceText: "“Em gửi chị 3.000đ tiền thừa và hóa đơn. Cảm ơn chị, hẹn gặp lại!”",
        isRight: true,
        feedback: "Hoàn tất",
        timestamp: 1005,
      },
      {
        sequence: 6,
        stage: "cooler",
        actionType: "inspect_cooler",
        targetKey: "cooler_display",
        choiceKey: "cooler_quarantine_expired",
        choiceText: "Lấy hộp hết hạn khỏi tủ",
        isRight: true,
        feedback: "Tốt",
        timestamp: 1006,
      },
      {
        sequence: 7,
        stage: "quarantine",
        actionType: "quarantine_product",
        targetKey: "quarantine_bin",
        choiceKey: "quarantine_label_expired",
        choiceText: "Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”.",
        isRight: true,
        feedback: "Tốt",
        timestamp: 1007,
      },
      {
        sequence: 8,
        stage: "mentor",
        actionType: "report_mentor",
        targetKey: "mentor_mai",
        choiceKey: "mentor_full_report",
        choiceText:
          "Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn, đã tách và gắn nhãn ở khu xử lý.",
        isRight: true,
        feedback: "Hoàn thành",
        timestamp: 1008,
      },
    ];

    // Gọi Oracle mô phỏng trình duyệt tự suy diễn tiến trình
    const resDerivedLegacy = evaluateLegacyBrowserSimulation(stableActions);
    expect(resDerivedLegacy.score).toBe(100);
    expect(resDerivedLegacy.errors).toBe(0);
    expect(resDerivedLegacy.passed).toBe(true);
    expect(resDerivedLegacy.completedSteps).toBe(6);
    expect(resDerivedLegacy.grade).toBe("EXCELLENT");

    // Gọi Oracle authoritative server
    const resDerivedAuth = evaluateTrainingSession(
      {
        catalogVersion: "1.0.0",
        actions: stableActions,
      },
      SCENARIO_CATALOG_V1,
    );
    expect(resDerivedAuth.score).toBe(100);
    expect(resDerivedAuth.passed).toBe(true);
    expect(resDerivedAuth.rating).toBe("EXCELLENT");
    expect(resDerivedAuth.completedStages).toEqual(REQUIRED_STAGES);

    // Ca 7: Chống gian lận (Client cố tình gửi isRight: true cho lựa chọn sai trong catalog)
    const spoofedActions: TrainingActionRecord[] = [
      ...stableActions,
      {
        sequence: 9,
        stage: "greet",
        actionType: "greet_customer",
        targetKey: "customer_linh",
        choiceKey: "greet_discount", // Là lựa chọn SAI theo SCENARIO_CHOICE_MAP
        choiceText: "Chị cứ lấy loại đang giảm giá đi ạ.",
        isRight: true, // CLIENT GIAN LẬN: TỰ GỬI isRight: true
        feedback: "Khuyến mãi chưa chắc phù hợp",
        timestamp: 1009,
      },
    ];
    const resSpoofedLegacy = evaluateLegacyBrowserSimulation(spoofedActions);
    expect(resSpoofedLegacy.errors).toBe(1); // Oracle tra cứu catalog và phát hiện lỗi
    expect(resSpoofedLegacy.score).toBe(90);
    expect(resSpoofedLegacy.grade).toBe("SATISFACTORY");

    const resSpoofedAuth = evaluateTrainingSession(
      {
        catalogVersion: "1.0.0",
        actions: spoofedActions,
      },
      SCENARIO_CATALOG_V1,
    );
    expect(resSpoofedAuth.rightCount).toBe(8);
    expect(resSpoofedAuth.totalActions).toBe(9);
    expect(resSpoofedAuth.passed).toBe(true);
    expect(resSpoofedAuth.rating).toBe("GOOD");
  });

  test("Luồng hoàn thành đúng hoàn hảo (Happy Path) đạt 100 điểm và không gọi API vận hành", async ({
    page,
  }) => {
    const operationalWriteRequests: string[] = [];

    // Giám sát request ghi vận hành
    page.on("request", (req) => {
      const method = req.method().toUpperCase();
      const url = req.url().toLowerCase();
      const isWriteMethod = ["POST", "PUT", "PATCH", "DELETE"].includes(method);
      const isOperationalEndpoint =
        url.includes("/api/sales") ||
        url.includes("/api/invoices") ||
        url.includes("/api/inventory") ||
        url.includes("/api/receiving") ||
        url.includes("/api/stocktake") ||
        url.includes("/api/catalog");

      if (isWriteMethod && isOperationalEndpoint) {
        operationalWriteRequests.push(`${method} ${req.url()}`);
      }
    });

    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto(TRAINING_ROUTE);

    // Mở ca thực hành
    await expect(page.getByTestId("trn-scene-root")).toBeVisible();
    await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();

    // Bước 1: Gặp khách Linh (greet)
    await page.getByTestId("npc-customer").click();
    const rightChoice1 = page.getByRole("button", {
      name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/,
    });
    await expect(rightChoice1).toBeVisible({ timeout: 5000 });
    await rightChoice1.click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Linh: Đúng rồi");
    await page.getByTestId("btn-dialogue-next").click();

    // Bước 2: Kệ sữa (milk)
    await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
    const rightChoice2 = page.getByRole("button", {
      name: /Lấy 2 hộp sữa ít đường · 8.500đ \/ hộp/,
    });
    await expect(rightChoice2).toBeVisible({ timeout: 5000 });
    await rightChoice2.click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Bạn đặt 2 hộp sữa ít đường");
    await page.getByTestId("btn-dialogue-next").click();
    await expect(page.locator(".bag-panel")).toContainText("× 2");

    // Bước 3: Quầy thu ngân (checkout)
    await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 2", exact: true }).click();
    await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
    const rightChoice3 = page.getByRole("button", {
      name: /Em gửi chị 3.000đ tiền thừa và hóa đơn/,
    });
    await expect(rightChoice3).toBeVisible({ timeout: 5000 });
    await rightChoice3.click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Linh: Cảm ơn em nhé");
    await page.getByTestId("btn-dialogue-next").click();

    // Bước 4: Tủ mát (cooler)
    await page.getByRole("button", { name: "Đi đến Tủ mát", exact: true }).click();
    const rightChoice4 = page.getByRole("button", {
      name: /Lấy hộp hết hạn khỏi tủ và mang tới khu hàng cần xử lý/,
    });
    await expect(rightChoice4).toBeVisible({ timeout: 5000 });
    await rightChoice4.click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Bạn lấy hộp sữa chua hết hạn");
    await page.getByTestId("btn-dialogue-next").click();

    // Bước 5: Khu xử lý (quarantine)
    await page.getByRole("button", { name: "Đi đến Khu hàng cần xử lý", exact: true }).click();
    const rightChoice5 = page.getByRole("button", {
      name: /Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”/,
    });
    await expect(rightChoice5).toBeVisible({ timeout: 5000 });
    await rightChoice5.click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Hàng đã được tách khỏi khu bán",
    );
    await page.getByTestId("btn-dialogue-next").click();

    // Bước 6: Báo cáo Mentor Mai (mentor)
    await page.getByTestId("npc-mentor").click();
    const rightChoice6 = page.getByRole("button", {
      name: /Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn/,
    });
    await expect(rightChoice6).toBeVisible({ timeout: 5000 });
    await rightChoice6.click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Mai: Tốt lắm!");
    await page.getByTestId("btn-dialogue-next").click();

    // Xác nhận màn hình Tổng kết ca học
    await expect(page.getByTestId("modal-complete")).toBeVisible();
    await expect(page.getByTestId("evaluation-score")).toContainText("100 / 100 điểm");
    await expect(page.getByTestId("evaluation-grade")).toContainText("Xuất sắc");
    await expect(page.getByTestId("evaluation-errors")).toContainText(
      "Hoàn thành đúng ngay từ lần đầu",
    );
    await expect(page.getByTestId("btn-replay")).toBeVisible();
    await expect(page.getByTestId("btn-finish")).toBeVisible();

    // Khẳng định không có bất kỳ request ghi nào xuống API vận hành
    expect(operationalWriteRequests).toEqual([]);
  });

  test("Luồng phản hồi sai và sửa sai: Mentor hướng dẫn, trừ điểm Oracle và ghi nhận số lỗi", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto(TRAINING_ROUTE);
    await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();

    // 1. Bước greet: chọn sai -> nhận phản hồi -> chọn lại đúng
    await page.getByTestId("npc-customer").click();
    await page.getByRole("button", { name: /Chị cứ lấy loại đang giảm giá đi ạ/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Mai: Khuyến mãi chưa chắc phù hợp",
    );
    await page
      .getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/ })
      .click();
    await page.getByTestId("btn-dialogue-next").click();

    // 2. Bước milk: chọn sai -> nhận phản hồi -> chọn lại đúng
    await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
    await page.getByRole("button", { name: /Lấy 2 hộp sữa có đường/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Mai: Loại này có đường");
    await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường · 8.500đ/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    // 3. Bước checkout: quét 2 hộp -> chọn sai tiền thừa -> nhận phản hồi -> chọn lại đúng
    await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 2", exact: true }).click();
    await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
    await page.getByRole("button", { name: /Trả 2.000đ, cảm ơn khách/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Mai: Hãy tính lại: 20.000 − 17.000",
    );
    await page.getByRole("button", { name: /Em gửi chị 3.000đ tiền thừa và hóa đơn/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    // 4. Bước cooler: chọn sai -> nhận phản hồi -> chọn lại đúng
    await page.getByRole("button", { name: "Đi đến Tủ mát", exact: true }).click();
    await page.getByRole("button", { name: /Giảm giá để bán nhanh trong hôm nay/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Mai: Hàng đã hết hạn không được tiếp tục bán",
    );
    await page.getByRole("button", { name: /Lấy hộp hết hạn khỏi tủ/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    // 5. Bước quarantine: chọn sai -> nhận phản hồi -> chọn lại đúng
    await page.getByRole("button", { name: "Đi đến Khu hàng cần xử lý", exact: true }).click();
    await page.getByRole("button", { name: /Đặt chung vào thùng hàng mới nhận/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Mai: Hàng mới và hàng không đủ điều kiện bán phải tách riêng",
    );
    await page.getByRole("button", { name: /Đặt vào khay riêng, gắn nhãn/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    // 6. Bước mentor: chọn sai -> nhận phản hồi -> chọn lại đúng
    await page.getByTestId("npc-mentor").click();
    await page.getByRole("button", { name: /Không có gì cần báo chị ạ/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText(
      "Mai: Hàng đã tách cần được bàn giao",
    );
    await page
      .getByRole("button", { name: /Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn/ })
      .click();
    await page.getByTestId("btn-dialogue-next").click();

    // Tổng kết: 6 lỗi sai -> Điểm: 100 - 6 * 10 = 40 điểm, xếp loại "Cần luyện tập thêm"
    await expect(page.getByTestId("modal-complete")).toBeVisible();
    await expect(page.getByTestId("evaluation-score")).toContainText("40 / 100 điểm");
    await expect(page.getByTestId("evaluation-grade")).toContainText("Cần luyện tập thêm");
    await expect(page.getByTestId("evaluation-errors")).toContainText(
      "6 lần được mentor hướng dẫn lại",
    );
  });

  test("Luồng bỏ dở ca học (Abandon) hiển thị hộp thoại xác nhận và bảo toàn trạng thái", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto(TRAINING_ROUTE);
    await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();

    // Đang trong ca học, nhấn nút thoát
    await page.getByTestId("btn-exit-shift").click();
    const modalPrompt = page.getByTestId("modal-exit-prompt");
    await expect(modalPrompt).toBeVisible();
    await expect(modalPrompt).toContainText("Rời ca thực hành?");
    await expect(modalPrompt).toContainText("Dữ liệu cửa hàng không bị ảnh hưởng");

    // Chọn "Tiếp tục chơi" -> modal đóng lại, vẫn ở trong game
    await page.getByTestId("btn-cancel-exit").click();
    await expect(modalPrompt).toBeHidden();
    await expect(page.getByTestId("trn-scene-root")).toBeVisible();

    // Nhấn lại thoát -> chọn "Rời ca"
    await page.getByTestId("btn-exit-shift").click();
    await page.getByTestId("btn-confirm-exit").click();

    // Thoát về giao diện quản lý / đăng nhập
    await expect(page.getByTestId("modal-exit-prompt")).toBeHidden();
  });

  test("Luồng chơi lại ca học (Replay) đặt lại toàn bộ trạng thái phiên sạch sẽ", async ({
    page,
  }) => {
    test.setTimeout(60000);
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto(TRAINING_ROUTE);
    await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();

    // Đi nhanh qua 6 bước để tới màn hình hoàn thành
    await page.getByTestId("npc-customer").click();
    await page
      .getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/ })
      .click();
    await page.getByTestId("btn-dialogue-next").click();

    await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
    await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường · 8.500đ/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
    await page.getByRole("button", { name: "Quét hộp sữa 2", exact: true }).click();
    await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
    await page.getByRole("button", { name: /Em gửi chị 3.000đ tiền thừa và hóa đơn/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    await page.getByRole("button", { name: "Đi đến Tủ mát", exact: true }).click();
    await page.getByRole("button", { name: /Lấy hộp hết hạn khỏi tủ/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    await page.getByRole("button", { name: "Đi đến Khu hàng cần xử lý", exact: true }).click();
    await page.getByRole("button", { name: /Đặt vào khay riêng, gắn nhãn/ }).click();
    await page.getByTestId("btn-dialogue-next").click();

    await page.getByTestId("npc-mentor").click();
    await page
      .getByRole("button", { name: /Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn/ })
      .click();
    await page.getByTestId("btn-dialogue-next").click();

    // Kiểm tra màn hình hoàn thành
    await expect(page.getByTestId("modal-complete")).toBeVisible();

    // Bấm nút "Chơi lại ca này"
    await page.getByTestId("btn-replay").click();

    // Màn hình quay về intro chào mừng ca mới
    await expect(page.getByTestId("modal-complete")).toBeHidden();
    const startShiftBtn = page.getByRole("button", { name: /Bắt đầu ca thực hành/ });
    await expect(startShiftBtn).toBeVisible();

    // Bắt đầu lại ca mới: nhân vật quay về vị trí ban đầu (10, 11) và stage = greet
    await startShiftBtn.click();
    const player = page.locator(".world-player");
    await expect(player).toHaveAttribute("data-x", "10");
    await expect(player).toHaveAttribute("data-y", "11");
    await expect(player).toHaveAttribute("data-stage", "greet");
    await expect(page.locator(".bag-panel")).toContainText("Chưa mang theo sản phẩm");
  });
});

const MOCK_SERVER_CATALOG = {
  version: "v1",
  requiredStages: ["greeting", "consultation", "checkout"] as const,
  choicesByKey: {
    greet_warm: { choiceKey: "greet_warm", stage: "greeting", isRight: true },
    consult_need: { choiceKey: "consult_need", stage: "consultation", isRight: true },
    scan_item_1: {
      choiceKey: "scan_item_1",
      stage: "checkout",
      actionType: "scan_item",
      isRight: true,
    },
    scan_item_2: {
      choiceKey: "scan_item_2",
      stage: "checkout",
      actionType: "scan_item",
      isRight: true,
    },
    tender_3k_receipt: {
      choiceKey: "tender_3k_receipt",
      stage: "checkout",
      actionType: "tender",
      isRight: true,
    },
  },
};

test.describe("TRN-03 Authoritative Training Oracle", () => {
  test("Ca 1: Từ chối chuỗi 8 sự kiện dùng choiceKey không tồn tại dù client truyền isRight=true và completedStages", () => {
    const forgedEvents = Array.from({ length: 8 }, (_, idx) => ({
      choiceKey: `non_existent_key_${idx + 1}`,
      isRight: true,
      stage: idx < 2 ? "greeting" : idx < 5 ? "consultation" : "checkout",
      actionType: "scan_item",
      choiceText: "Client forged text",
    }));

    expect(() =>
      evaluateTrainingSession(
        {
          catalogVersion: "v1",
          actions: forgedEvents,
          completedStages: ["greeting", "consultation", "checkout"],
        },
        MOCK_SERVER_CATALOG,
      ),
    ).toThrow(TrainingCatalogValidationError);

    expect(() =>
      resolveActionRight({ choiceKey: "non_existent_key", isRight: true }, MOCK_SERVER_CATALOG),
    ).toThrow(TrainingCatalogValidationError);
  });

  test("Ca 1b: Từ chối khi thiếu choiceKey hoặc sai catalogVersion", () => {
    expect(() =>
      evaluateTrainingSession(
        {
          catalogVersion: "invalid_version",
          actions: [{ choiceKey: "greet_warm", isRight: true }],
        },
        MOCK_SERVER_CATALOG,
      ),
    ).toThrow(/Unsupported catalogVersion/);

    expect(() =>
      evaluateTrainingSession(
        {
          catalogVersion: "v1",
          actions: [{ isRight: true, stage: "greeting" }],
        },
        MOCK_SERVER_CATALOG,
      ),
    ).toThrow(/missing required choiceKey/);
  });

  test("Ca 2: Không tính hoàn thành bước checkout và không đạt 100 điểm nếu quét trùng scan_item_1 hai lần mà thiếu scan_item_2", () => {
    const duplicateScanActions = [
      { choiceKey: "greet_warm" },
      { choiceKey: "consult_need" },
      { choiceKey: "scan_item_1" },
      { choiceKey: "scan_item_1" }, // Quét trùng mã 1, thiếu scan_item_2
      { choiceKey: "tender_3k_receipt" },
    ];

    const completedStages = deriveCompletedStages(duplicateScanActions, MOCK_SERVER_CATALOG);
    expect(completedStages).not.toContain("checkout");
    expect(completedStages).toEqual(["greeting", "consultation"]);

    const result = evaluateTrainingSession(
      {
        catalogVersion: "v1",
        actions: duplicateScanActions,
        // Cố tình truyền completedStages giả từ client để đảm bảo server phớt lờ
        completedStages: ["greeting", "consultation", "checkout"],
      },
      MOCK_SERVER_CATALOG,
    );

    expect(result.completedStages).not.toContain("checkout");
    expect(result.passed).toBe(false);
    expect(result.score).toBeLessThan(100);
    expect(result.rating).not.toBe("EXCELLENT");
  });

  test("Happy Path: Đạt 100 điểm, passed=true, EXCELLENT khi có đủ scan_item_1, scan_item_2 và tender_3k_receipt hợp lệ từ catalog", () => {
    const validActions = [
      { choiceKey: "greet_warm", isRight: false }, // Server phải lấy isRight=true từ catalog bất kể client gửi false
      { choiceKey: "consult_need" },
      { choiceKey: "scan_item_1" },
      { choiceKey: "scan_item_2" },
      { choiceKey: "tender_3k_receipt" },
    ];

    const result = evaluateTrainingSession(
      {
        catalogVersion: "v1",
        actions: validActions,
      },
      MOCK_SERVER_CATALOG,
    );

    expect(result.completedStages).toEqual(["greeting", "consultation", "checkout"]);
    expect(result.score).toBe(100);
    expect(result.passed).toBe(true);
    expect(result.rating).toBe("EXCELLENT");
  });
});
