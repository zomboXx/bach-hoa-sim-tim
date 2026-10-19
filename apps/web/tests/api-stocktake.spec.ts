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
  permissions: ["inventory.read", "stocktakes.read", "stocktakes.write"],
};

const productPage = {
  items: [
    {
      productId,
      sku: "GAO-001",
      productName: "Gạo thơm 5kg",
      unitCode: "KG",
      productStatus: "ACTIVE",
      onHandQuantity: "12.500",
      availableQuantity: "12.500",
      version: 7,
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
      version: 7,
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

test("training with an API session never replays a pending stocktake", async ({ page }) => {
  await installLogin(page, { ...stockSession, trainingEnabled: true });
  const operationalWrites: string[] = [];
  page.on("request", (request) => {
    if (
      /^(POST|PUT|PATCH|DELETE)$/.test(request.method()) &&
      /\/api\/v1\/(?:inventory|sales|receipts)(?:\/|$)/.test(new URL(request.url()).pathname)
    ) {
      operationalWrites.push(`${request.method()} ${request.url()}`);
    }
  });
  await signIn(page);
  await expect(page.locator("nav")).toBeVisible();

  await page.evaluate(
    async ({ actorId, organizationId, storeId, productId, batchId }) => {
      await new Promise<void>((resolve, reject) => {
        const request = indexedDB.open("simtim-stocktake-queue-v1", 1);
        request.onupgradeneeded = () =>
          request.result.createObjectStore("operations", { keyPath: "clientOperationId" });
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
          const db = request.result;
          const tx = db.transaction("operations", "readwrite");
          tx.objectStore("operations").put({
            clientOperationId: "api-training-pending",
            actorId,
            organizationId,
            storeId,
            countedAt: new Date().toISOString(),
            productId,
            batchId,
            expectedQuantity: 12.5,
            baseVersion: 7,
            actualQuantity: 11.75,
            note: "Chờ đồng bộ ngoài ca học",
            status: "PENDING",
          });
          tx.oncomplete = () => {
            db.close();
            resolve();
          };
          tx.onerror = () => reject(tx.error);
        };
      });
    },
    {
      actorId: stockSession.userId,
      organizationId: stockSession.organizationId,
      storeId,
      productId,
      batchId,
    },
  );

  await page.locator("nav").getByRole("button", { name: "Đào tạo nghiệp vụ" }).click();
  await expect(page.getByTestId("trn-scene-root")).toBeVisible();
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
  await page.getByRole("button", { name: /Bắt đầu ca thực hành/ }).click();
  expect(operationalWrites).toEqual([]);
  const queued = await page.evaluate(async () => {
    return new Promise<unknown>((resolve, reject) => {
      const request = indexedDB.open("simtim-stocktake-queue-v1", 1);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        const item = db
          .transaction("operations")
          .objectStore("operations")
          .get("api-training-pending");
        item.onsuccess = () => {
          resolve(item.result);
          db.close();
        };
        item.onerror = () => reject(item.error);
      };
    });
  });
  expect(queued).toMatchObject({ clientOperationId: "api-training-pending", status: "PENDING" });
});

test("API mode submits the store-scoped batch snapshot through the accepted provider contract", async ({
  page,
}) => {
  await installLogin(page);
  const sessionId = "70000000-0000-0000-0000-000000000001";
  let submitted: Record<string, unknown> | undefined;
  let releaseProducts: (() => void) | undefined;
  await page.route("**/api/v1/inventory/products?*", async (route) => {
    await new Promise<void>((resolve) => (releaseProducts = resolve));
    await json(route, 200, productPage);
  });
  await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, batchPage));
  await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, emptyPage));
  await page.route("**/api/v1/inventory/stocktakes", (route) =>
    json(route, 201, {
      id: sessionId,
      status: "OPEN",
      lines: [],
    }),
  );
  await page.route(`**/api/v1/inventory/stocktakes/${sessionId}/counts`, async (route) => {
    submitted = route.request().postDataJSON() as Record<string, unknown>;
    await json(route, 200, {
      id: "70000000-0000-0000-0000-000000000002",
      stocktakeId: sessionId,
      productId,
      batchId,
      clientOperationId: submitted.clientOperationId,
      expectedQuantity: 12.5,
      actualQuantity: 11.75,
      baseVersion: 7,
      note: "Đếm tại quầy",
      status: "PENDING",
      conflictReason: null,
      countedAt: submitted.countedAt,
    });
  });

  await signIn(page);
  await openStocktake(page);
  await expect(page.getByRole("status")).toContainText("Đang tải lô hàng");
  releaseProducts?.();

  await expect(page.getByRole("heading", { name: "Ghi số đếm" })).toBeVisible();
  await expect(page.getByText(storeId, { exact: true })).toBeVisible();
  await expect(page.getByRole("option", { name: "Gạo thơm 5kg · BATCH-001" })).toHaveCount(1);
  await expect(page.getByLabel("Lô hàng")).toHaveValue(batchId);
  await expect(page.getByRole("button", { name: "Gửi phiếu kiểm kê" })).toBeEnabled();
  await page.getByLabel("Số lượng thực tế").fill("11.750");
  await page.getByLabel("Ghi chú", { exact: true }).fill("Đếm tại quầy");
  await page.getByRole("button", { name: "Gửi phiếu kiểm kê" }).click();
  await expect(page.locator(".count-row")).toContainText("Chờ duyệt");
  expect(submitted).toMatchObject({
    batchId,
    actualQuantity: 11.75,
    baseVersion: 7,
    note: "Đếm tại quầy",
  });
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
