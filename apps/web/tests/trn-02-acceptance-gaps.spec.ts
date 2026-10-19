import { expect, test, type Page } from "@playwright/test";

const TRAINING_ROUTE = process.env.TRN_ROUTE || "/training";

async function startShift(page: Page) {
  await page.goto(TRAINING_ROUTE);
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
}

type MistakeMode = "none" | "greeting" | "all";

async function completeScenario(page: Page, mistakes: MistakeMode = "none") {
  await page.getByTestId("npc-customer").click();
  if (mistakes !== "none") {
    await page.getByRole("button", { name: /Chị cứ lấy loại đang giảm giá/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Khuyến mãi chưa chắc phù hợp");
  }
  await page.getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/ }).click();
  await page.getByTestId("btn-dialogue-next").click();

  await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
  if (mistakes === "all") {
    await page.getByRole("button", { name: /Lấy 2 hộp sữa có đường/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("Loại này có đường");
  }
  await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường · 8.500đ/ }).click();
  await page.getByTestId("btn-dialogue-next").click();

  await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
  await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
  await page.getByRole("button", { name: "Quét hộp sữa 2", exact: true }).click();
  await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
  if (mistakes === "all") {
    await page.getByRole("button", { name: /Trả 2.000đ, cảm ơn khách/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("20.000 − 17.000");
  }
  await page.getByRole("button", { name: /Em gửi chị 3.000đ tiền thừa và hóa đơn/ }).click();
  await page.getByTestId("btn-dialogue-next").click();

  await page.getByRole("button", { name: "Đi đến Tủ mát", exact: true }).click();
  if (mistakes === "all") {
    await page.getByRole("button", { name: /Giảm giá để bán nhanh/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("không được tiếp tục bán");
  }
  await page.getByRole("button", { name: /Lấy hộp hết hạn khỏi tủ/ }).click();
  await page.getByTestId("btn-dialogue-next").click();

  await page.getByRole("button", { name: "Đi đến Khu hàng cần xử lý", exact: true }).click();
  if (mistakes === "all") {
    await page.getByRole("button", { name: /Đặt chung vào thùng hàng mới nhận/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("phải tách riêng");
  }
  await page.getByRole("button", { name: /Đặt vào khay riêng, gắn nhãn/ }).click();
  await page.getByTestId("btn-dialogue-next").click();

  await page.getByTestId("npc-mentor").click();
  if (mistakes === "all") {
    await page.getByRole("button", { name: /Không có gì cần báo chị ạ/ }).click();
    await expect(page.getByTestId("choice-feedback")).toContainText("cần được bàn giao");
  }
  await page
    .getByRole("button", { name: /Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn/ })
    .click();
  await page.getByTestId("btn-dialogue-next").click();
  await expect(page.getByTestId("modal-complete")).toBeVisible();
}

async function verifyOrderedFlow(page: Page) {
  await startShift(page);
  const player = page.locator(".world-player");

  // Đi sai mục tiêu: Mentor và quầy chỉ nhắc mục tiêu hiện tại, không tiến stage.
  await page.getByTestId("npc-mentor").click();
  await expect(
    page.locator(".conversation-content").getByText(/Đến gặp khách Linh ở cửa ra vào/),
  ).toBeVisible();
  await page.getByRole("button", { name: /Mình hiểu rồi/ }).click();
  await page.getByTestId("btn-dialogue-next").click();
  await expect(player).toHaveAttribute("data-stage", "greet");

  await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
  await expect(
    page.locator(".conversation-content").getByText(/Đến gặp khách Linh ở cửa ra vào/),
  ).toBeVisible();
  await page.getByRole("button", { name: /Mình hiểu rồi/ }).click();
  await page.getByTestId("btn-dialogue-next").click();
  await expect(player).toHaveAttribute("data-stage", "greet");

  // Đi đúng tới checkout nhưng mới quét 1/2 sản phẩm thì chưa được nhận tiền.
  await page.getByTestId("npc-customer").click();
  await page.getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ/ }).click();
  await page.getByTestId("btn-dialogue-next").click();
  await page.getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true }).click();
  await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường · 8.500đ/ }).click();
  await page.getByTestId("btn-dialogue-next").click();
  await page.getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true }).click();
  await page.getByRole("button", { name: "Quét hộp sữa 1", exact: true }).click();
  await expect(page.getByRole("button", { name: /Nhận tiền từ khách/ })).toBeDisabled();
  await expect(player).toHaveAttribute("data-stage", "checkout");
}

test.describe("TRN-02 acceptance gaps", () => {
  for (const viewport of [
    { name: "desktop", width: 1280, height: 720 },
    { name: "mobile", width: 390, height: 844 },
  ]) {
    test(`TRN02-M04 không bỏ qua stage và khóa nhận tiền (${viewport.name})`, async ({ page }) => {
      test.setTimeout(60000);
      await page.setViewportSize(viewport);
      await verifyOrderedFlow(page);
    });
  }

  test("TRN02-M03 mobile phản hồi đủ 6 lỗi và chấm 40 điểm", async ({ page }) => {
    test.setTimeout(60000);
    await page.setViewportSize({ width: 390, height: 844 });
    await startShift(page);
    await completeScenario(page, "all");
    await expect(page.getByTestId("evaluation-score")).toContainText("40 / 100 điểm");
    await expect(page.getByTestId("evaluation-grade")).toContainText("Cần luyện tập thêm");
    await expect(page.getByTestId("evaluation-errors")).toContainText(
      "6 lần được mentor hướng dẫn lại",
    );
  });

  test("TRN02-M06 chơi lại xóa lỗi cũ và lần hai chấm lại 100 điểm", async ({ page }) => {
    test.setTimeout(90000);
    await page.setViewportSize({ width: 390, height: 844 });
    await startShift(page);

    await completeScenario(page, "greeting");
    await expect(page.getByTestId("evaluation-score")).toContainText("90 / 100 điểm");
    await expect(page.getByTestId("evaluation-errors")).toContainText(
      "1 lần được mentor hướng dẫn lại",
    );

    await page.getByTestId("btn-replay").click();
    await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
    await completeScenario(page);

    await expect(page.getByTestId("evaluation-score")).toContainText("100 / 100 điểm");
    await expect(page.getByTestId("evaluation-grade")).toContainText("Xuất sắc");
    await expect(page.getByTestId("evaluation-errors")).toContainText(
      "Hoàn thành đúng ngay từ lần đầu",
    );
  });
});
