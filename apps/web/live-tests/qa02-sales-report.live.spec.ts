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
  const salesWrites: string[] = [];
  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    if (
      request.method() === "POST" &&
      ["/api/v1/sales/quote", "/api/v1/sales/checkout"].includes(path)
    ) {
      salesWrites.push(path);
    }
  });

  await signIn(page, "sales");
  const nav = page.locator("nav");
  await expect(nav.getByRole("button", { name: "Bán hàng", exact: false })).toBeVisible();
  await expect(nav.getByRole("button", { name: "Hóa đơn", exact: false })).toBeVisible();
  await nav.getByRole("button", { name: "Bán hàng", exact: false }).click();
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("1000000");
  await page.getByRole("button", { name: "Xác nhận thanh toán", exact: false }).click();

  await expect
    .poll(
      () =>
        salesWrites.includes("/api/v1/sales/quote") &&
        salesWrites.includes("/api/v1/sales/checkout"),
    )
    .toBe(true);
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
