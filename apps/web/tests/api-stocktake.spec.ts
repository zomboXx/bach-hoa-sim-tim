import { expect, test, type Page, type Route } from "@playwright/test";

const token = "s".repeat(43);
const productId = "10000000-0000-0000-0000-000000000041";
const batchId = "50000000-0000-0000-0000-000000000001";
const storeId = "10000000-0000-0000-0000-000000000002";

const stockSession = {
  userId: "40000000-0000-0000-0000-000000000001",
  organizationId: "10000000-0000-0000-0000-000000000001",
  storeId,
  fullName: "Nhân viên kho",
  trainingEnabled: false,
  roles: ["STOCK"],
  permissions: ["inventory.read"],
};

const productPage = {
  items: [
    {
      productId,
      sku: "GAO-001",
      productName: "Gạo thơm 5kg",
      productStatus: "ACTIVE",
      onHandQuantity: "12.500",
      availableQuantity: "12.500",
      businessDate: "2026-10-09",
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
};

const batchPage = {
  items: [
    {
      batchId,
      productId,
      batchNumber: "BATCH-001",
      supplierLotNumber: "LOT-2026-10",
      status: "AVAILABLE",
      expiryStatus: "VALID",
      expiryDate: "2027-04-01",
      receivedDate: "2026-10-03",
      onHandQuantity: "12.500",
      availableQuantity: "12.500",
      businessDate: "2026-10-09",
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
};

const emptyPage = {
  items: [],
  page: 0,
  size: 100,
  totalElements: 0,
  totalPages: 0,
};

function grant(session = stockSession) {
  return {
    accessToken: token,
    tokenType: "Bearer",
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    session,
  };
}

async function json(route: Route, status: number, body?: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

async function installLogin(page: Page, session = stockSession) {
  await page.route("**/api/v1/auth/login", (route) => json(route, 200, grant(session)));
}

async function signIn(page: Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openStocktake(page: Page) {
  await page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }).click();
}

test("API mode shows loading, store-scoped batch data and the provider boundary", async ({
  page,
}) => {
  await installLogin(page);
  let releaseProducts: (() => void) | undefined;
  await page.route("**/api/v1/inventory/products?*", async (route) => {
    await new Promise<void>((resolve) => (releaseProducts = resolve));
    await json(route, 200, productPage);
  });
  await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, batchPage));
  await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, emptyPage));

  await signIn(page);
  await openStocktake(page);
  await expect(page.getByRole("status")).toContainText("Đang tải lô hàng");
  releaseProducts?.();

  await expect(page.getByRole("heading", { name: "Ghi số đếm" })).toBeVisible();
  await expect(page.getByText(storeId, { exact: true })).toBeVisible();
  await expect(page.getByRole("option", { name: "Gạo thơm 5kg · BATCH-001" })).toHaveCount(1);
  await expect(page.getByLabel("Lô hàng")).toHaveValue(batchId);
  await expect(page.getByRole("alert")).toContainText("API ghi kiểm kê chưa được bật");
  await expect(page.getByRole("button", { name: "Gửi phiếu kiểm kê" })).toBeDisabled();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
    ),
  ).toBe(true);
});

test("API inventory 403 stays in-session and explains the missing permission", async ({ page }) => {
  await installLogin(page);
  await page.route("**/api/v1/inventory/products?*", (route) =>
    json(route, 403, {
      code: "FORBIDDEN",
      message: "Thiếu quyền inventory.read",
    }),
  );
  await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, batchPage));
  await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, emptyPage));

  await signIn(page);
  await openStocktake(page);
  await expect(page.getByRole("alert")).toContainText("Thiếu quyền inventory.read");
  await expect(page.locator("nav")).toBeVisible();
});

test("API session without inventory.read cannot open stocktake", async ({ page }) => {
  await installLogin(page, { ...stockSession, permissions: [] });
  await signIn(page);
  await expect(
    page.locator("nav").getByRole("button", { name: "Kiểm kê", exact: false }),
  ).toHaveCount(0);
});
