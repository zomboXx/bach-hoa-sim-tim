import { expect, test, type Page } from "@playwright/test";
import { cameraFromImage } from "../tests/helpers/camera";

test("@pos phone camera and desktop share a cart, retry and checkout once", async ({
  browser,
  baseURL,
}) => {
  test.setTimeout(60000);
  const password = process.env.SIMTIM_LIVE_PASSWORD;
  test.skip(!password, "Requires a live disposable backend");
  const phoneContext = await browser.newContext({
    baseURL,
    viewport: { width: 390, height: 844 },
    serviceWorkers: "block",
  });
  const deskContext = await browser.newContext({
    baseURL,
    viewport: { width: 1440, height: 1050 },
    serviceWorkers: "block",
  });
  const phone = await phoneContext.newPage();
  const desk = await deskContext.newPage();
  await cameraFromImage(phone, "/demo-barcodes/api-rice.png");
  async function signIn(page: Page) {
    await page.goto("/");
    await page.getByLabel("Mã nhân viên", { exact: true }).fill("sales");
    await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
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
  async function expectQuantity(page: Page, value: string) {
    await expect(page.locator(".cart-row .stepper input")).toHaveValue(value, { timeout: 5000 });
  }
  try {
    await Promise.all([signIn(phone), signIn(desk)]);
    await expect(desk.locator(".product-grid .product-card")).toHaveCount(10);
    const started = Date.now();
    await phone.getByRole("button", { name: /Quét mã bằng camera/ }).click();
    await expect(phone.getByRole("dialog").locator("strong")).toHaveText("8930000000001");
    await phone.getByRole("button", { name: "Đóng camera" }).click();
    await expectQuantity(phone, "1");
    await expectQuantity(desk, "1");
    console.log(
      "POS camera to second device observed in",
      Date.now() - started,
      "ms (includes camera startup)",
    );
    const search = desk.getByLabel("Tìm sản phẩm bán hàng");
    await search.fill("8930000000001");
    await search.press("Enter");
    await expectQuantity(phone, "2");
    await expectQuantity(desk, "2");
    await Promise.all([
      phone.getByRole("button", { name: "Tăng Gạo gói 1 kg" }).click(),
      desk.getByRole("button", { name: "Tăng Gạo gói 1 kg" }).click(),
    ]);
    await expectQuantity(phone, "4");
    await expectQuantity(desk, "4");

    const requests: string[] = [];
    await phone.route("**/api/v1/sales/cart/operations", async (route) => {
      requests.push(route.request().postData()!);
      const response = await route.fetch();
      if (requests.length === 1 && response.ok()) {
        await route.fulfill({
          status: 502,
          contentType: "application/json",
          body: '{"code":"LOST_RESPONSE"}',
        });
      } else await route.fulfill({ response });
    });
    await phone.getByRole("button", { name: "Tăng Gạo gói 1 kg" }).click();
    await expectQuantity(desk, "5");
    await expect(
      phone.getByText("Chưa rõ thao tác đã lưu hay chưa.", { exact: false }),
    ).toBeVisible();
    await phone.getByRole("button", { name: "Thử đồng bộ lại" }).click();
    await expectQuantity(phone, "5");
    expect(requests).toHaveLength(2);
    expect(requests[0]).toBe(requests[1]);

    await desk.getByLabel("Khách đưa").fill("1000000");
    const response = desk.waitForResponse(
      (res) => new URL(res.url()).pathname === "/api/v1/sales/checkout" && res.status() === 201,
    );
    await desk.getByRole("button", { name: /Xác nhận thanh toán/ }).click();
    const invoice = await (await response).json();
    expect(invoice.lines).toHaveLength(1);
    expect(Number(invoice.lines[0].quantity)).toBe(5);
    await expect(desk.locator(".invoice-row").first()).toContainText(invoice.invoiceNo);
    await expect(phone.locator(".cart-row")).toHaveCount(0);
    await expect(phone.getByRole("button", { name: /Xác nhận thanh toán/ })).toBeDisabled();
    console.log(
      "POS shared checkout",
      invoice.id,
      "quantity=5, invoice visible and both carts reset",
    );
  } finally {
    await Promise.allSettled([phoneContext.close(), deskContext.close()]);
  }
});
