import { test, expect } from "@playwright/test";

test.describe("Reports API Adapter", () => {
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
            permissions: ["reports.read", "catalog.read"],
          },
        },
      });
    });

    await page.goto("/?api=true");
    await page.getByLabel("Mã nhân viên", { exact: true }).fill("manager");
    await page.getByLabel("Mật khẩu", { exact: true }).fill("password");
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();

    await expect(page.locator("text=Tổng quan").first()).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 200, json: { revenue: 0, invoiceCount: 0 } });
    });
    await page.route("**/api/v1/reports/inventory*", async (route) => {
      await route.fulfill({ status: 200, json: { items: [] } });
    });

    await page.getByRole("button", { name: /Báo cáo/ }).click();
    await expect(
      page.locator("text=Chưa có dữ liệu báo cáo trong khoảng thời gian này.").first(),
    ).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 500 });
    });
    await page.fill('input[type="date"]', "2026-09-01");
    await expect(page.locator("text=Đã xảy ra lỗi khi tải dữ liệu báo cáo.").first()).toBeVisible();

    await page.route("**/api/v1/reports/revenue*", async (route) => {
      await route.fulfill({ status: 200, json: { revenue: 1500000, invoiceCount: 5 } });
    });
    await page.route("**/api/v1/reports/inventory*", async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          items: [
            { productId: "p1", sku: "SKU1", name: "Product 1", quantity: 10, status: "NORMAL" },
          ],
        },
      });
    });

    await page.getByRole("button", { name: "Thử lại" }).click();
    await expect(page.locator("text=1.500.000").first()).toBeVisible();
    await expect(page.locator("text=Product 1").first()).toBeVisible();
  });
});

test.describe("Vietnam report date", () => {
  test.use({ timezoneId: "America/Los_Angeles" });

  test("uses the Vietnam business day and an exclusive next-day boundary", async ({ page }) => {
    await page.clock.install({ time: new Date("2026-10-19T18:30:00Z") });
    await page.route("**/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          accessToken: "mocked-token-for-testing-only-with-43-chars",
          tokenType: "Bearer",
          expiresAt: "2026-10-21T00:00:00Z",
          session: {
            userId: "00000000-0000-0000-0000-000000000001",
            organizationId: "00000000-0000-0000-0000-000000000001",
            storeId: "00000000-0000-0000-0000-000000000001",
            fullName: "Manager User",
            trainingEnabled: false,
            roles: ["MANAGER"],
            permissions: ["reports.read"],
          },
        },
      });
    });
    await page.route("**/api/v1/reports/revenue*", (route) =>
      route.fulfill({ status: 200, json: { revenue: 0, invoiceCount: 0 } }),
    );
    await page.route("**/api/v1/reports/inventory*", (route) =>
      route.fulfill({ status: 200, json: { items: [] } }),
    );

    await page.goto("/");
    await page.getByLabel("Mã nhân viên", { exact: true }).fill("manager");
    await page.getByLabel("Mật khẩu", { exact: true }).fill("password");
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
    const revenueRequest = page.waitForRequest("**/api/v1/reports/revenue*");
    await page.getByRole("button", { name: /Báo cáo/ }).click();
    const url = new URL((await revenueRequest).url());
    expect(url.searchParams.get("from")).toBe("2026-10-20");
    expect(url.searchParams.get("to")).toBe("2026-10-21");
  });
});
