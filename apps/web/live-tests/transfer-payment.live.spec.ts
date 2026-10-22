import { expect, test, type Page } from "@playwright/test";
import { BinaryBitmap, HybridBinarizer, QRCodeReader, RGBLuminanceSource } from "@zxing/library";

test("@transfer QR survives reload and two devices complete one persisted sale after response loss", async ({
  browser,
  baseURL,
}) => {
  test.setTimeout(90000);
  const password = process.env.SIMTIM_LIVE_PASSWORD;
  test.skip(!password, "Requires disposable PostgreSQL and test beneficiary configuration");
  const phoneContext = await browser.newContext({
    baseURL,
    viewport: { width: 390, height: 844 },
    serviceWorkers: "block",
  });
  const desktopContext = await browser.newContext({
    baseURL,
    viewport: { width: 1440, height: 1050 },
    serviceWorkers: "block",
  });
  const phone = await phoneContext.newPage();
  const desktop = await desktopContext.newPage();
  async function openSale(page: Page) {
    await page.locator("aside.sidebar").waitFor({ state: "attached" });
    const toggle = page.getByRole("button", { name: "Mở menu" });
    if (await toggle.isVisible()) await toggle.click();
    await page
      .locator("nav")
      .getByRole("button", { name: /Bán hàng/ })
      .click();
    await expect(
      page.getByRole("status").filter({ hasText: "Giỏ chung đã kết nối" }),
    ).toBeVisible();
  }
  async function login(page: Page) {
    await page.goto("/");
    await page.getByLabel("Mã nhân viên", { exact: true }).fill("sales");
    await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
    await openSale(page);
  }
  try {
    await Promise.all([login(phone), login(desktop)]);
    await phone.getByLabel("Tìm sản phẩm bán hàng").fill("8930000000001");
    await phone.getByLabel("Tìm sản phẩm bán hàng").press("Enter");
    await expect(desktop.locator(".cart-row .stepper span")).toHaveText("1");
    await expect(desktop.locator(".total b")).toContainText("22.500");
    await desktop.getByLabel("Phương thức").selectOption("Chuyển khoản QR");
    const creation = desktop.waitForResponse(
      (res) =>
        new URL(res.url()).pathname === "/api/v1/sales/transfers" &&
        res.request().method() === "POST",
    );
    await desktop.getByRole("button", { name: "Tạo QR và giữ hàng" }).click();
    const order = await (await creation).json();
    expect(order.status).toBe("PENDING");
    expect(order.accountName).toBe("TEST ONLY - DO NOT TRANSFER");
    await expect(phone.locator(".transfer-detail h3")).toHaveText("Chờ xác nhận tiền về");
    await expect(phone.getByRole("button", { name: "Tăng Gạo gói 1 kg" })).toBeDisabled();
    await expect(desktop.getByRole("button", { name: "Tăng Gạo gói 1 kg" })).toBeDisabled();
    const svg = desktop.locator(".transfer-qr svg");
    await expect(svg).toBeVisible();
    // Decode the actual rendered SVG independently from the server's TLV encoder.
    const rectangles = await svg
      .locator("rect")
      .evaluateAll((nodes) =>
        nodes.map((node) =>
          ["x", "y", "width", "height"].map((attr) => Number(node.getAttribute(attr))),
        ),
      );
    const pixels = new Uint8ClampedArray(256 * 256).fill(255);
    for (const [x, y, width, height] of rectangles) {
      for (let row = y; row < y + height; row++)
        for (let column = x; column < x + width; column++) pixels[row * 256 + column] = 0;
    }
    const decoded = new QRCodeReader().decode(
      new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(pixels, 256, 256))),
    );
    expect(decoded.getText()).toBe(order.qrPayload);
    expect(decoded.getText()).toContain(order.reference);
    expect(decoded.getText()).toContain("540522500");
    await phone.reload();
    // Bearer tokens intentionally stay in memory; authenticate again after a full reload.
    await login(phone);
    await expect(phone.locator(".transfer-detail")).toContainText(order.reference);
    await expect(phone.getByLabel("Phương thức")).toHaveValue("Chuyển khoản QR");
    await expect(phone.getByLabel("Khách đưa")).toHaveCount(0);
    await expect(phone.locator(".transfer-qr svg")).toBeVisible();
    expect(await phone.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await phone.screenshot({
      path: test.info().outputPath("pending-qr-mobile.png"),
      fullPage: true,
    });

    let confirmations = 0;
    await desktop.route("**/api/v1/sales/transfers/*/confirm", async (route) => {
      confirmations++;
      const response = await route.fetch();
      if (confirmations === 1)
        await route.fulfill({
          status: 502,
          contentType: "application/json",
          body: '{"message":"Lost response after commit"}',
        });
      else await route.fulfill({ response });
    });
    await desktop.getByLabel("Số tiền thực nhận (VND)").fill("22500");
    await desktop.getByLabel("Mã giao dịch ngân hàng", { exact: true }).fill("LIVE-TRANSFER-001");
    await desktop.getByRole("checkbox", { name: /Tôi đã kiểm tra tiền về/ }).check();
    await desktop.getByRole("button", { name: "Xác nhận đã nhận tiền" }).click();
    await expect(phone.locator(".transfer-detail h3")).toHaveText("Đã nhận tiền");
    await expect(desktop.locator(".transfer-detail h3")).toHaveText("Đã nhận tiền");
    await expect(phone.locator(".cart-row")).toHaveCount(0);
    await expect(desktop.locator(".cart-row")).toHaveCount(0);
    await expect(desktop.locator(".transfer-qr svg")).toHaveCount(0);
    await desktop.getByText("Lịch sử chuyển khoản", { exact: true }).click();
    await expect(desktop.locator(".transfer-history")).toContainText("1 đã nhận");
    await expect(desktop.locator(".transfer-history")).toContainText("22.500");
    expect(confirmations).toBe(1);
    console.log(
      "Manual transfer",
      order.id,
      "QR decoded, both carts locked/reset, confirmation response loss recovered",
    );
  } finally {
    await Promise.allSettled([phoneContext.close(), desktopContext.close()]);
  }
});
