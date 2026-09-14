import { test, expect } from "@playwright/test";
test("changed inventory requires recount instead of overwriting stock", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Quản lý cửa hàng QL001" }).click();
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  const nav = page.locator("nav");
  await nav.getByRole("button", { name: /Kiểm kê/ }).click();
  await page.getByLabel("Số lượng thực tế").fill("44");
  await page.getByRole("button", { name: /Gửi phiếu kiểm kê/ }).click();
  await expect(page.locator(".count-row")).toContainText("Chờ duyệt");
  await nav.getByRole("button", { name: /Bán hàng/ }).click();
  await page.getByRole("button", { name: /Sữa tươi ít đường/ }).click();
  await page.getByLabel("Khách đưa").fill("10000");
  await page.getByRole("button", { name: "Xác nhận thanh toán" }).click();
  await expect(page.locator(".invoice-row")).toHaveCount(1);
  await nav.getByRole("button", { name: /Kiểm kê/ }).click();
  await page.getByRole("button", { name: "Duyệt điều chỉnh tồn" }).click();
  await expect(page.locator(".count-row")).toContainText("Tồn đã đổi");
  await nav.getByRole("button", { name: /Tồn kho & lô hàng/ }).click();
  await expect(page.locator("tr").filter({ hasText: "LO01" })).toContainText("47");
});
test("receive → sell → invoice → offline count → approval; training isolation", async ({
  page,
  context,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await page.getByRole("button", { name: "Quản lý cửa hàng QL001" }).click();
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  const nav = page.locator("nav");
  await nav.getByRole("button", { name: "Nhận hàng", exact: false }).click();
  await page.getByLabel("Mã lô", { exact: true }).fill("TEST-LOT");
  await page.getByLabel("Hạn sử dụng", { exact: true }).fill("2099-12-31");
  await page.getByLabel("Lý do từ chối / ghi chú").fill("2 hộp rách bao bì");
  await page.getByRole("button", { name: "Xác nhận nhận 18 sản phẩm" }).click();
  await expect(
    page.getByText("Đã nhận", { exact: false }).last(),
  ).toBeVisible();
  await nav
    .getByRole("button", { name: "Tồn kho & lô hàng", exact: false })
    .click();
  await expect(
    page.locator("tr").filter({ hasText: "TEST-LOT" }),
  ).toContainText("18");
  await nav.getByRole("button", { name: "Bán hàng", exact: false }).click();
  await page.getByRole("button", { name: /Sữa tươi ít đường/ }).click();
  await page.getByLabel("Khách đưa").fill("10000");
  await page.getByRole("button", { name: "Xác nhận thanh toán" }).click();
  await expect(page.locator(".invoice-row")).toHaveCount(1);
  await expect(page.locator(".invoice-row")).toContainText("7.650");
  await page.locator(".invoice-row").click();
  await expect(page.locator("dialog")).toContainText("Sữa tươi ít đường × 1");
  await page.getByRole("button", { name: "Đóng hóa đơn" }).click();
  await nav
    .getByRole("button", { name: "Tồn kho & lô hàng", exact: false })
    .click();
  await expect(page.locator("tr").filter({ hasText: "LO01" })).toContainText(
    "47",
  );
  await expect(page.locator("tr").filter({ hasText: "LO07" })).toContainText(
    "6",
  );
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Mở menu", exact: true }).click();
  await nav.getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await context.setOffline(true);
  await expect(page.locator(".connection")).toContainText("Ngoại tuyến");
  await page.getByLabel("Số lượng thực tế").fill("46");
  await page
    .getByLabel("Ghi chú", { exact: true })
    .fill("Kiểm kê offline tại kệ");
  await page.getByRole("button", { name: "Lưu phiếu offline" }).click();
  await expect(page.locator(".count-row")).toContainText("Chờ kết nối");
  await page.reload();
  await page.getByRole("button", { name: "Mở menu", exact: true }).click();
  await nav.getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await expect(page.locator(".count-row")).toContainText(
    "Kiểm kê offline tại kệ",
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await context.setOffline(false);
  await expect(page.locator(".count-row")).toContainText("Chờ duyệt");
  await page.getByRole("button", { name: "Duyệt điều chỉnh tồn" }).click();
  await expect(page.locator(".count-row")).toContainText("Đã duyệt");
  await page.getByRole("button", { name: "Mở menu", exact: true }).click();
  await nav
    .getByRole("button", { name: "Đào tạo nghiệp vụ", exact: false })
    .click();
  await page.getByRole("button", { name: "Kiểm tra kiện hàng" }).click();
  await page.getByRole("button", { name: "Ghi nhận hàng đạt yêu cầu" }).click();
  await page.getByRole("button", { name: "Xác nhận nhập kho" }).click();
  await expect(page.getByRole("status").first()).toContainText(
    "Có 2 hộp bị hỏng",
  );
  await page.getByLabel("Số lượng nhập kho").fill("18");
  await page.getByRole("button", { name: "Xác nhận nhập kho" }).click();
  await expect(
    page.getByText("Hoàn thành bài thực hành!", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Mở menu", exact: true }).click();
  await nav
    .getByRole("button", { name: "Tồn kho & lô hàng", exact: false })
    .click();
  await expect(page.locator("tr").filter({ hasText: "LO01" })).toContainText(
    "46",
  );
  await expect(
    page.locator("tr").filter({ hasText: "TEST-LOT" }),
  ).toContainText("18");
  expect(errors).toEqual([]);
});
test("demo login and sales role hide managerial controls", async ({ page }) => {
  await page.goto("/");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("wrong");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.getByRole("alert")).toContainText("chưa đúng");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("demo123");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(
    page
      .locator("nav")
      .getByRole("button", { name: /Nhận hàng|Báo cáo|Khuyến mãi|Kiểm kê/ }),
  ).toHaveCount(0);
  await page
    .locator("nav")
    .getByRole("button", { name: "Sản phẩm", exact: false })
    .click();
  await expect(
    page.getByRole("button", { name: "Thêm sản phẩm", exact: false }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Đăng xuất", exact: false }).click();
  await expect(
    page.getByRole("button", { name: "Vào không gian làm việc" }),
  ).toBeVisible();
});
