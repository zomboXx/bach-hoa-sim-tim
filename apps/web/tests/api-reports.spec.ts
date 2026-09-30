import { test, expect } from "@playwright/test";

test.describe("Reports API Adapter", () => {
  test.use({ baseURL: "http://localhost:5173" }); 

  test("should show loading, empty and error states for reports", async ({ page }) => {
    await page.route("**/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          accessToken: "mocked-token-for-testing-only-with-43-chars",
          tokenType: "Bearer",
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
          session: {
            userId: "00000000-0000-0000-0000-000000000001",
            organizationId: "00000000-0000-0000-0000-000000000001",
            storeId: "00000000-0000-0000-0000-000000000001",
            fullName: "Manager User",
            trainingEnabled: false,
            roles: ["MANAGER"],
            permissions: ["reports.read", "catalog.read"]
          }
        }
      });
    });

    await page.goto("/?api=true");
    await page.fill('input[type="text"]', "manager");
    await page.fill('input[type="password"]', "password");
    await page.click('button:has-text("Đăng nhập")');

    await expect(page.locator("text=Tổng quan")).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 200, json: { revenue: 0, invoiceCount: 0 } });
    });
    await page.route("**/api/v1/reports/inventory*", async (route) => {
      await route.fulfill({ status: 200, json: { items: [] } });
    });

    await page.click("text=Báo cáo");
    await expect(page.locator("text=Chưa có dữ liệu báo cáo trong khoảng thời gian này.")).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 500 });
    });
    await page.fill('input[type="date"]', "2026-09-01");
    await expect(page.locator("text=Đã xảy ra lỗi khi tải dữ liệu báo cáo.")).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 200, json: { revenue: 1500000, invoiceCount: 5 } });
    });
    await page.route("**/api/v1/reports/inventory*", async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          items: [
            { productId: "p1", sku: "SKU1", name: "Product 1", quantity: 10, status: "NORMAL" }
          ]
        }
      });
    });

    await page.click("button:has-text('Thử lại')");
    await expect(page.locator("text=1.500.000")).toBeVisible();
    await expect(page.locator("text=Product 1")).toBeVisible();
  });
});
