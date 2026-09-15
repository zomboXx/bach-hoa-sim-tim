import { test, expect, type Page } from "@playwright/test";

test("touch controls stop on release; exit and re-entry reset the practice session", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page
    .getByRole("button", { name: "Mở đào tạo cùng Mentor Mai", exact: true })
    .click();
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
  const control = await page
    .getByRole("button", { name: "Đi phải", exact: true })
    .boundingBox();
  await page.mouse.move(
    control!.x + control!.width / 2,
    control!.y + control!.height / 2,
  );
  await page.mouse.down();
  await expect(page.locator(".world-player")).not.toHaveAttribute(
    "data-x",
    "10",
  );
  await page.mouse.up();
  const x = await page.locator(".world-player").getAttribute("data-x");
  await page.waitForTimeout(300); // More than two movement ticks: releasing must stop motion.
  await expect(page.locator(".world-player")).toHaveAttribute("data-x", x!);
  await page.getByRole("button", { name: "Về cửa hàng", exact: false }).click();
  await expect(
    page.getByRole("heading", { name: "Rời ca thực hành?" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Tiếp tục chơi", exact: true })
    .click();
  await expect(page.locator(".world-player")).toHaveAttribute("data-x", x!);
  await page.getByRole("button", { name: "Về cửa hàng", exact: false }).click();
  await page.getByRole("button", { name: "Rời ca", exact: true }).click();
  await page
    .getByRole("button", { name: "Mở đào tạo cùng Mentor Mai", exact: true })
    .click();
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
  await expect(page.locator(".world-player")).toHaveAttribute("data-x", "10");
  await expect(page.locator(".world-player")).toHaveAttribute(
    "data-stage",
    "greet",
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

async function playShift(page: Page) {
  await page
    .getByRole("button", { name: "Bắt đầu ca thực hành", exact: false })
    .click();
  const game = page.locator(".world-player");
  await expect(game).toHaveAttribute("data-x", "10");
  await page.keyboard.press("ArrowDown");
  await expect(game).toHaveAttribute("data-y", "12");
  await page.keyboard.press("ArrowDown");
  await expect(game).toHaveAttribute("data-y", "12"); // wall collision
  for (let i = 0; i < 4; i++) await page.keyboard.press("ArrowRight");
  for (let i = 0; i < 2; i++) await page.keyboard.press("ArrowUp");
  await page.keyboard.press("e");
  await page
    .getByRole("button", { name: /Chị cứ lấy loại đang giảm giá/ })
    .click();
  await expect(page.locator(".choice-feedback")).toContainText(
    "chưa chắc phù hợp",
  );
  await expect(game).toHaveAttribute("data-stage", "greet");
  await page
    .getByRole("button", { name: /Dạ, chị muốn 2 hộp sữa ít đường/ })
    .click();
  await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
  await page
    .getByRole("button", { name: "Đi đến Kệ sữa & đồ uống", exact: true })
    .click();
  await page.getByRole("button", { name: /Lấy 2 hộp sữa ít đường/ }).click();
  await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
  await expect(page.locator(".bag-panel")).toContainText("× 2");
  await page
    .getByRole("button", { name: "Đi đến Quầy thanh toán", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Quét hộp sữa 1", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: /Nhận tiền từ khách/ }),
  ).toBeDisabled();
  await page
    .getByRole("button", { name: "Quét hộp sữa 2", exact: true })
    .click();
  await page.getByRole("button", { name: /Nhận tiền từ khách/ }).click();
  await page.getByRole("button", { name: /Trả 2.000đ/ }).click();
  await expect(page.locator(".choice-feedback")).toContainText("3.000");
  await page
    .getByRole("button", { name: /Em gửi chị 3.000đ tiền thừa/ })
    .click();
  await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
  await page
    .getByRole("button", { name: "Đi đến Tủ mát", exact: true })
    .click();
  await page.getByRole("button", { name: /Giảm giá để bán nhanh/ }).click();
  await expect(page.locator(".choice-feedback")).toContainText(
    "không được tiếp tục bán",
  );
  await page.getByRole("button", { name: /Lấy hộp hết hạn khỏi tủ/ }).click();
  await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
  await expect(page.locator(".bag-panel")).toContainText("Sữa chua hết hạn");
  await page
    .getByRole("button", { name: "Đi đến Khu hàng cần xử lý", exact: true })
    .click();
  await page.getByRole("button", { name: /Đặt vào khay riêng/ }).click();
  await page.getByRole("button", { name: /Tiếp tục trong cửa hàng/ }).click();
  await page
    .getByRole("button", { name: "Đi đến Mentor Mai", exact: true })
    .click();
  await page
    .getByRole("button", { name: /Em đã bán đúng hàng cho khách/ })
    .click();
  await page.getByRole("button", { name: /Kết thúc ca/ }).click();
  await expect(
    page.getByRole("heading", { name: "Một ca làm, ba kỹ năng." }),
  ).toBeVisible();
  await expect(page.locator(".complete-skills")).toContainText(
    "Giao tiếp & tư vấn",
  );
  await expect(page.locator(".complete-skills")).toContainText(
    "Thanh toán & tiền thừa",
  );
  await expect(page.locator(".complete-skills")).toContainText(
    "Hàng hết hạn & bàn giao",
  );
}
test("changed inventory requires recount instead of overwriting stock", async ({
  page,
}) => {
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
  await expect(page.locator("tr").filter({ hasText: "LO01" })).toContainText(
    "47",
  );
});
test("receive → sell → invoice → offline count → approval; training isolation", async ({
  page,
  context,
}) => {
  test.setTimeout(90000);
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
  await playShift(page);
  await page
    .getByRole("button", { name: "Trở về cửa hàng", exact: false })
    .click();
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
