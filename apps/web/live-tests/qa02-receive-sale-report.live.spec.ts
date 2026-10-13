import { expect, test, type Page } from "@playwright/test";

const password = process.env.SIMTIM_LIVE_PASSWORD;
if (!password) throw new Error("SIMTIM_LIVE_PASSWORD is required for live backend tests.");

async function signIn(page: Page, username: "stock" | "sales" | "manager") {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill(username);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function signOut(page: Page) {
  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await expect(page.getByLabel("Mã nhân viên", { exact: true })).toBeVisible();
}

test("@qa02-gap06 QA02-WEB-004: receive, CASH sale and reports reconcile on one clean backend", async ({
  page,
}) => {
  let receiptPostCount = 0;
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      new URL(request.url()).pathname === "/api/v1/inventory/receipts"
    ) {
      receiptPostCount++;
    }
  });

  await signIn(page, "stock");
  await page
    .locator("nav")
    .getByRole("button", { name: /Nhận hàng/ })
    .click();
  await expect(page.getByRole("heading", { name: "Phiếu nhận hàng mới" })).toBeVisible();
  const receivedProductLabel = await page
    .getByLabel("Sản phẩm")
    .locator("option:checked")
    .innerText();
  const receivedProductName = receivedProductLabel.split(" · ").at(-1)!;
  await page.getByLabel("Giá nhập (VND)").fill("24500");
  await page.getByLabel(/Số lô nhà cung cấp/).fill(`QA06-${crypto.randomUUID().slice(0, 8)}`);
  const expiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  await page.getByLabel(/Hạn sử dụng/).fill(expiry);

  const receiptResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      new URL(response.url()).pathname === "/api/v1/inventory/receipts",
  );
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  const receiptResponse = await receiptResponsePromise;
  expect(receiptResponse.status()).toBe(201);
  expect(receiptPostCount).toBe(1);
  await expect(page.getByRole("status")).toContainText("Đã xác nhận phiếu");

  await signOut(page);
  await signIn(page, "sales");
  await page.locator("nav").getByRole("button", { name: "Bán hàng", exact: false }).click();
  await page.getByLabel("Tìm sản phẩm bán hàng").fill(receivedProductName);
  const receivedProductCard = page
    .locator(".product-card")
    .filter({ hasText: receivedProductName });
  await expect(receivedProductCard).toHaveCount(1);
  await receivedProductCard.click();
  await page.getByLabel("Khách đưa").fill("1000000");

  const checkoutResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      new URL(response.url()).pathname === "/api/v1/sales/checkout",
  );
  await page.getByRole("button", { name: "Xác nhận thanh toán", exact: false }).click();
  const checkoutResponse = await checkoutResponsePromise;
  expect(checkoutResponse.status()).toBe(201);
  const invoice = (await checkoutResponse.json()) as { grandTotal: number; id: string };
  expect(invoice.grandTotal).toBeGreaterThan(0);
  expect(invoice.id).toMatch(/^[0-9a-f-]{36}$/i);
  await expect(page.locator(".invoice-row").first()).toBeVisible();

  await signOut(page);
  await signIn(page, "manager");
  const revenueResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      new URL(response.url()).pathname === "/api/v1/reports/revenue",
  );
  const inventoryResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      new URL(response.url()).pathname === "/api/v1/reports/inventory",
  );
  await page.locator("nav").getByRole("button", { name: "Báo cáo", exact: false }).click();
  const [revenueResponse, inventoryResponse] = await Promise.all([
    revenueResponsePromise,
    inventoryResponsePromise,
  ]);
  expect(revenueResponse.status()).toBe(200);
  expect(inventoryResponse.status()).toBe(200);

  const revenue = (await revenueResponse.json()) as { invoiceCount: number; revenue: number };
  const inventory = (await inventoryResponse.json()) as { items: unknown[] };
  expect(revenue.invoiceCount).toBe(1);
  expect(revenue.revenue).toBe(invoice.grandTotal);
  expect(inventory.items.length).toBeGreaterThan(0);
  await expect(page.getByRole("heading", { name: "Báo cáo cửa hàng" })).toBeVisible();
  await expect(page.getByText("Đã xảy ra lỗi khi tải dữ liệu báo cáo.")).toHaveCount(0);
});
