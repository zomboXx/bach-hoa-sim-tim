import { expect, test, type Page } from "@playwright/test";

const password = process.env.SIMTIM_LIVE_PASSWORD;
if (!password) throw new Error("SIMTIM_LIVE_PASSWORD is required for live backend tests.");

async function signIn(page: Page, username: "sales" | "manager") {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill(username);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

test("@qa02-gap05 QA02-WEB-002: sales uses live quote, CASH checkout and invoice APIs", async ({
  page,
}) => {
  const quoteCalls: string[] = [];
  const checkoutCalls: { url: string; idempotencyKey: string | null }[] = [];
  const invoiceGetCalls: string[] = [];

  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    if (request.method() === "POST" && path === "/api/v1/sales/quote") {
      quoteCalls.push(path);
    }
    if (request.method() === "POST" && path === "/api/v1/sales/checkout") {
      checkoutCalls.push({
        url: path,
        idempotencyKey: request.headers()["idempotency-key"] ?? null,
      });
    }
    if (request.method() === "GET" && path === "/api/v1/sales/invoices") {
      invoiceGetCalls.push(path);
    }
  });

  await signIn(page, "sales");
  const nav = page.locator("nav");
  await expect(nav.getByRole("button", { name: "Bán hàng", exact: false })).toBeVisible();
  await expect(nav.getByRole("button", { name: "Hóa đơn", exact: false })).toBeVisible();

  // Navigate to sale, add first available product, fill cash and confirm
  await nav.getByRole("button", { name: "Bán hàng", exact: false }).click();
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await expect(page.locator(".total b")).not.toContainText(/^0/);
  await page.getByLabel("Khách đưa").fill("1000000");
  await page.getByRole("button", { name: "Xác nhận thanh toán", exact: false }).click();

  // Both quote and checkout must have fired
  await expect
    .poll(() => quoteCalls.length > 0 && checkoutCalls.length > 0, { timeout: 10_000 })
    .toBe(true);

  // Checkout must carry an Idempotency-Key UUID header (S2-GAP-04 contract)
  expect(checkoutCalls[0].idempotencyKey).toMatch(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  );

  // After checkout, invoices list is fetched and first row is visible
  await expect.poll(() => invoiceGetCalls.length > 0, { timeout: 10_000 }).toBe(true);
  await expect(page.locator(".invoice-row").first()).toBeVisible();

  // Navigate to Hóa đơn again to verify list persists
  await nav.getByRole("button", { name: "Hóa đơn", exact: false }).click();
  await expect(page.locator(".invoice-row").first()).toBeVisible();
});

test("@qa02-gap05 QA02-WEB-003: HTTP 502 after commit retries one persisted operation", async ({
  page,
}) => {
  const checkoutRequests: Array<{ key: string | undefined; body: string | null }> = [];
  await page.route("**/api/v1/sales/checkout", async (route) => {
    checkoutRequests.push({
      key: route.request().headers()["idempotency-key"],
      body: route.request().postData(),
    });
    const resp = await route.fetch();
    // The first request commits on the real server, but the proxy reports 502.
    if (checkoutRequests.length === 1 && resp.ok()) {
      await route.fulfill({
        status: 502,
        contentType: "application/json",
        body: JSON.stringify({
          code: "TEMPORARILY_UNAVAILABLE",
          message: "Upstream response was lost after commit",
        }),
      });
    } else {
      await route.fulfill({ response: resp });
    }
  });

  await signIn(page, "sales");
  await page.locator("nav").getByRole("button", { name: "Bán hàng", exact: false }).click();
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await expect(page.locator(".total b")).not.toContainText(/^0/);
  await page.getByLabel("Khách đưa").fill("1000000");
  await page.getByRole("button", { name: "Xác nhận thanh toán", exact: false }).click();

  await expect.poll(() => checkoutRequests.length, { timeout: 10_000 }).toBe(1);
  await expect(page.getByText(/502.*chưa chắc chắn/i)).toBeVisible();
  await expect(page.getByText(/lần thử tiếp theo sẽ dùng đúng mã giao dịch/i)).toBeVisible();
  await expect(page.getByLabel("Khách đưa")).toBeDisabled();
  await page.getByRole("button", { name: /Thử lại cùng mã giao dịch/ }).click();

  await expect.poll(() => checkoutRequests.length, { timeout: 10_000 }).toBe(2);
  expect(checkoutRequests[0].key).toMatch(/^[0-9a-f-]{36}$/i);
  expect(checkoutRequests[1].key).toBe(checkoutRequests[0].key);
  expect(checkoutRequests[1].body).toBe(checkoutRequests[0].body);
  await expect(page.locator(".invoice-row").first()).toBeVisible();
});

test("QA02-WEB-001: manager report renders responses from the live API", async ({ page }) => {
  const reportResponses: string[] = [];
  page.on("response", (response) => {
    const path = new URL(response.url()).pathname;
    if (response.ok() && ["/api/v1/reports/revenue", "/api/v1/reports/inventory"].includes(path)) {
      reportResponses.push(path);
    }
  });

  await signIn(page, "manager");
  await page.locator("nav").getByRole("button", { name: "Báo cáo", exact: false }).click();

  await expect
    .poll(
      () =>
        reportResponses.includes("/api/v1/reports/inventory") &&
        reportResponses.includes("/api/v1/reports/revenue"),
    )
    .toBe(true);
  await expect(page.getByRole("heading", { name: "Báo cáo cửa hàng" })).toBeVisible();
  await expect(page.getByText("Đã xảy ra lỗi khi tải dữ liệu báo cáo.")).toHaveCount(0);
});
