import { test, expect, type Page } from "@playwright/test";
async function login(page: Page, id = "NV001") {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên").fill(id);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.locator(".workspace")).toBeVisible();
}

for (const mobile of [false, true]) {
  test(`invoice-first editor: search, digits-only quantity, zero removal, camera placeholder (${mobile ? "mobile" : "desktop"})`, async ({
    page,
  }) => {
    if (mobile) await page.setViewportSize({ width: 390, height: 844 });
    await login(page);
    await expect(
      page.getByRole("heading", { name: "Hóa đơn đang lập", exact: true }),
    ).toBeVisible();
    await expect(page.getByText("Hóa đơn chưa có sản phẩm", { exact: true })).toBeVisible();
    await expect(page.locator(".cart-row")).toHaveCount(0);
    await expect(page.locator(".product-grid")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Xác nhận thanh toán" })).toBeDisabled();
    await page.screenshot({
      path: `test-results/invoice-empty-${mobile ? "mobile" : "desktop"}.png`,
      fullPage: true,
    });
    const state = await (await page.request.get("/api/state")).json();
    const today = new Date().toLocaleDateString("sv-SE", {
      timeZone: "Asia/Ho_Chi_Minh",
    });
    const product = state.products.find(
      (p: { id: string; active: boolean }) =>
        p.active &&
        state.batches
          .filter(
            (b: { productId: string; expiry: string }) => b.productId === p.id && b.expiry >= today,
          )
          .reduce((sum: number, b: { quantity: number }) => sum + b.quantity, 0) >= 5,
    );
    expect(product).toBeTruthy();
    const search = page.getByLabel("Tìm sản phẩm bán hàng");
    await search.fill(product.barcode);
    await search.press("Enter");
    const row = page.locator(".cart-row");
    const quantity = row.getByRole("textbox");
    await expect(row).toHaveCount(1);
    await expect(quantity).toHaveValue("1");
    await expect(search).toHaveValue("");
    // A repeated scan updates the existing line exactly once.
    await search.fill(product.barcode);
    await search.press("Enter");
    await expect(quantity).toHaveValue("2");
    await expect(row).toHaveCount(1);
    await row.getByRole("button", { name: "Tăng " + product.name, exact: true }).click();
    await expect(quantity).toHaveValue("3");
    for (const invalid of ["abc", "-1", "1.5", "1e2", "2a", "9999999999999999999999999"]) {
      await quantity.fill(invalid);
      await expect(quantity).toHaveValue("3");
    }
    await quantity.evaluate((input: HTMLInputElement) => {
      input.value = "4x";
      input.dispatchEvent(
        new InputEvent("input", {
          bubbles: true,
          inputType: "insertFromPaste",
          data: "4x",
        }),
      );
    });
    await expect(quantity).toHaveValue("3");
    await quantity.fill("99999999");
    await expect(quantity).toHaveValue("3");
    await quantity.fill("");
    await search.focus();
    await expect(quantity).toHaveValue("3");
    await quantity.fill("4");
    await expect(quantity).toHaveValue("4");
    await row.getByRole("button", { name: "Giảm " + product.name, exact: true }).click();
    await expect(quantity).toHaveValue("3");
    const adjustedPrice = state.promotions.find(
      (p: { productId: string; end: string }) => p.productId === product.id && p.end >= today,
    );
    const unitPrice = adjustedPrice
      ? Math.round(product.price * (1 - adjustedPrice.percent / 100))
      : product.price;
    await expect(row.locator(".line-total")).toHaveText(
      new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(unitPrice * 3),
    );
    await page.screenshot({
      path: `test-results/invoice-filled-${mobile ? "mobile" : "desktop"}.png`,
      fullPage: true,
    });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await quantity.fill("0");
    await expect(row).toHaveCount(0);
    // Name lookup is explicit: choose a result, then subtract to zero.
    await search.fill(product.name);
    await page.getByRole("button", { name: "Thêm " + product.name, exact: true }).click();
    await expect(quantity).toHaveValue("1");
    await row.getByRole("button", { name: "Giảm " + product.name, exact: true }).click();
    await expect(row).toHaveCount(0);
    await search.fill(product.barcode);
    await search.press("Enter");
    await row
      .getByRole("button", {
        name: "Xóa " + product.name + " khỏi hóa đơn",
        exact: true,
      })
      .click();
    await expect(row).toHaveCount(0);
    await page.getByRole("button", { name: "Quét mã bằng camera", exact: false }).click();
    await expect(page.getByRole("dialog")).toContainText("chưa mở camera và chưa đồng bộ hóa đơn");
    await page.getByRole("button", { name: "Đã hiểu", exact: true }).click();
    await expect(page.getByRole("dialog")).not.toBeVisible();
  });
}
test("server login, sales permissions, sale persists after reload", async ({ page }) => {
  await login(page);
  await expect(
    page.locator("nav").getByRole("button", { name: "Nhận hàng", exact: false }),
  ).toHaveCount(0);
  await page.locator("nav").getByRole("button", { name: "Bán hàng", exact: false }).click();
  const catalog = await (await page.request.get("/api/products")).json();
  const stocked = catalog.find((p: { barcode: string; active: boolean }) => p.active && p.barcode);
  const barcodeInput = page.getByLabel("Tìm sản phẩm bán hàng");
  await barcodeInput.fill(stocked.barcode);
  await barcodeInput.press("Enter");
  await expect(page.locator(".cart-row")).toHaveCount(1);
  await expect(barcodeInput).toHaveValue("");
  await page.getByLabel("Khách đưa").fill("100000");
  await page.getByRole("button", { name: "Xác nhận thanh toán" }).click();
  await expect(page.getByRole("heading", { name: "Hóa đơn", exact: true })).toBeVisible();
  await expect(page.locator(".invoice-row").first()).toContainText("Lan Nguyễn");
  await page.reload();
  await page.locator("nav").getByRole("button", { name: "Hóa đơn", exact: false }).click();
  await expect(page.locator(".invoice-row").first()).toContainText("Lan Nguyễn");
});
test("chapter lobby, all six placeholders, actual Godot launch and mentor interaction", async ({
  page,
}) => {
  const failures: string[] = [];
  page.on("pageerror", (e) => failures.push(e.message));
  page.on("console", (m) => {
    if (
      m.type() === "error" &&
      /SCRIPT ERROR|Parse Error|Invalid access|Script inherits/.test(m.text())
    )
      failures.push(m.text());
  });
  await login(page);
  await page.locator(".mentor-fab").click();
  await expect(page.locator(".chapter-card")).toHaveCount(6);
  for (const card of await page.locator(".chapter-card").all()) {
    await card.click();
    await expect(page.getByRole("dialog")).toContainText(
      "Nội dung này đang được cập nhật, vui lòng thử lại sau",
    );
    await page.getByRole("button", { name: "Đã hiểu", exact: true }).click();
  }
  await page.screenshot({
    path: "test-results/academy-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: /Bắt đầu Chapter 0|Khám phá lại cửa hàng/ }).click();
  await expect(page.locator("iframe")).toBeVisible();
  await expect(page.locator(".game-loading")).toHaveCount(0, {
    timeout: 60000,
  });
  const canvas = page.frameLocator("iframe").locator("canvas");
  await canvas.click();
  await page.keyboard.press("e");
  await page.waitForTimeout(300);
  await page.screenshot({
    path: "test-results/godot-mentor.png",
    fullPage: true,
  });
  await page.keyboard.press("e");
  await expect(page.locator(".tour-guide li").first()).toHaveClass("visited");
  expect(failures).toEqual([]);
});
test("mobile staff UI and chapter lobby fit viewport", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.locator(".mentor-fab").click();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({
    path: "test-results/academy-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: /Bắt đầu Chapter 0|Khám phá lại cửa hàng/ }).click();
  await expect(page.locator("iframe")).toBeVisible();
  await expect(page.locator(".game-loading")).toHaveCount(0, {
    timeout: 60000,
  });
  await page.frameLocator("iframe").locator("canvas").click();
  await page.keyboard.press("e");
  await page.waitForTimeout(300);
  await page.screenshot({
    path: "test-results/godot-mobile.png",
    fullPage: true,
  });
});

test("manager catalog CRUD and stock receiving persist through the API", async ({ page }) => {
  await login(page, "QL001");
  const suffix = String(Date.now()).slice(-10);
  const category = "Nhóm thử " + suffix;
  const product = "Sản phẩm thử " + suffix;
  await page.locator("nav").getByRole("button", { name: "Sản phẩm", exact: false }).click();
  await page.getByRole("button", { name: "Thêm danh mục", exact: false }).click();
  await page.getByLabel("Tên danh mục").fill(category);
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await page.getByRole("button", { name: "Thêm sản phẩm", exact: false }).click();
  await page.getByLabel("Tên sản phẩm", { exact: true }).fill(product);
  await page.getByLabel("Mã vạch", { exact: true }).fill("893" + suffix);
  await page.getByLabel("Danh mục", { exact: true }).selectOption({ label: category });
  await page.getByLabel("Giá bán (đ)").fill("20000");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  const row = page.locator("tbody tr").filter({ hasText: product });
  await row.getByRole("button", { name: "Sửa", exact: true }).click();
  await page.getByLabel("Giá bán (đ)").fill("25000");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(row).toContainText("25.000");
  await page.locator("nav").getByRole("button", { name: "Nhà cung cấp", exact: false }).click();
  await page.getByRole("button", { name: "Thêm nhà cung cấp", exact: false }).click();
  await page.getByLabel("Tên nhà cung cấp").fill("Đối tác " + suffix);
  await page.getByLabel("Điện thoại").fill("0901234567");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await page.getByRole("button", { name: "Đăng xuất", exact: false }).click();
  await page.getByLabel("Mã nhân viên").fill("KHO001");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page.locator("nav").getByRole("button", { name: "Nhận hàng", exact: false }).click();
  await page
    .getByLabel("Nhà cung cấp", { exact: true })
    .selectOption({ label: "Đối tác " + suffix });
  await page.getByLabel("Sản phẩm", { exact: true }).selectOption({ label: product });
  await page.getByLabel("Mã lô", { exact: true }).fill("E2E-" + suffix);
  await page.getByLabel("Hạn sử dụng", { exact: true }).fill("2030-12-31");
  await page.getByLabel("Số lượng giao").fill("10");
  await page.getByLabel("Số lượng chấp nhận").fill("9");
  await page.getByLabel("Lý do từ chối / ghi chú").fill("Một hộp bị móp");
  await page.getByRole("button", { name: "Xác nhận nhận", exact: false }).click();
  await expect(page.locator(".receipt-row").filter({ hasText: product })).toContainText(
    "Đã nhận 9",
  );
  await page
    .locator("nav")
    .getByRole("button", { name: "Tồn kho & lô hàng", exact: false })
    .click();
  await expect(page.locator("tbody tr").filter({ hasText: "E2E-" + suffix })).toContainText("9");
});

test("manager offline count queue survives reload, syncs once, and requires explicit approval", async ({
  page,
  context,
}) => {
  await login(page, "QL001");
  await page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }).click();
  const note = "offline-" + Date.now();
  await context.setOffline(true);
  await page.getByLabel("Số lượng thực tế").fill("31");
  await page.getByLabel("Ghi chú", { exact: true }).fill(note);
  await page.getByRole("button", { name: "Lưu phiếu offline", exact: false }).click();
  await expect(page.locator(".count-row").filter({ hasText: note })).toContainText("Chờ kết nối");
  await page.reload();
  await context.setOffline(false);
  await page.goto("/");
  // Server session remains valid, queue is recovered by owner and auto-synced.
  await expect(page.locator(".workspace")).toBeVisible();
  await page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await expect(page.locator(".count-row").filter({ hasText: note })).toContainText("Chờ duyệt");
  await expect(page.locator(".count-row").filter({ hasText: note })).toHaveCount(1);
  const saved = page.locator(".count-row").filter({ hasText: note });
  await saved.getByRole("button", { name: "Duyệt điều chỉnh tồn" }).click();
  await expect(saved).toContainText("Đã duyệt");
});

for (const [id, allowed, forbidden] of [
  ["NV001", "Bán hàng", ["Nhận hàng", "Kiểm kê", "Không gian kế toán"]],
  ["KHO001", "Nhận hàng", ["Bán hàng", "Kiểm kê", "Không gian kế toán"]],
  ["KT001", "Không gian kế toán", ["Bán hàng", "Nhận hàng", "Kiểm kê", "Trung tâm phê duyệt"]],
  ["QL001", "Trung tâm phê duyệt", ["Bán hàng", "Nhận hàng", "Không gian kế toán"]],
  ["ADMIN001", "Chủ cửa hàng", []],
] as const) {
  test(`workspace boundaries and mentor return: ${id}`, async ({ page }) => {
    await login(page, id);
    await expect(
      page.locator("nav").getByRole("button", { name: allowed, exact: false }),
    ).toBeVisible();
    for (const label of forbidden) {
      await expect(
        page.locator("nav").getByRole("button", { name: label, exact: false }),
      ).toHaveCount(0);
    }
    await page.locator(".mentor-fab").click();
    await page.getByRole("button", { name: "Về cửa hàng", exact: false }).click();
    await expect(page.locator(".workspace h1").first()).toBeVisible();
    await page.reload();
    await expect(page.locator(".workspace h1").first()).toBeVisible();
    await page.setViewportSize({ width: 390, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  });
}
