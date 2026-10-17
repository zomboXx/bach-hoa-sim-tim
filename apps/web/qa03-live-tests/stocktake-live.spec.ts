import { mkdir } from "node:fs/promises";
import path from "node:path";
import { expect, test, type APIRequestContext, type Page, type Route } from "@playwright/test";

const password = process.env.SIMTIM_LIVE_PASSWORD;
if (!password) throw new Error("SIMTIM_LIVE_PASSWORD is required for QA-03 live tests.");

const organizationId = "10000000-0000-0000-0000-000000000001";
const storeId = "10000000-0000-0000-0000-000000000002";
const evidenceDir = path.resolve(process.cwd(), "../../docs/testing/evidence/qa-03");

interface Grant {
  accessToken: string;
  session: {
    organizationId: string;
    storeId: string;
  };
}

test.describe.configure({ mode: "serial" });

async function apiLogin(request: APIRequestContext, username: string): Promise<Grant> {
  const response = await request.post("/api/v1/auth/login", {
    data: {
      organizationCode: "SIMTIM",
      storeCode: "MAIN",
      username,
      password,
    },
  });
  expect(response.status()).toBe(200);
  return (await response.json()) as Grant;
}

function headers(grant: Grant) {
  return {
    Authorization: `Bearer ${grant.accessToken}`,
    "X-Organization-Id": grant.session.organizationId,
    "X-Store-Id": grant.session.storeId,
  };
}

async function browserLogin(page: Page, username: string) {
  await page.getByLabel("Mã nhân viên", { exact: true }).fill(username);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password!);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.locator("nav")).toBeVisible();
}

async function openStocktake(page: Page) {
  const nav = page.locator("nav");
  const menu = page.getByRole("button", { name: "Mở menu", exact: true });
  if (await menu.isVisible()) await menu.click();
  await nav.getByRole("button", { name: "Kiểm kê", exact: false }).click();
  await expect(page.getByRole("heading", { name: "Kiểm kê theo lô" })).toBeVisible();
}

async function pageItems(
  request: APIRequestContext,
  path: string,
  grant: Grant,
): Promise<Record<string, unknown>[]> {
  const response = await request.get(path, { headers: headers(grant) });
  expect(response.status()).toBe(200);
  const body = (await response.json()) as { items: Record<string, unknown>[] };
  return body.items;
}

test("mobile offline count survives reload, retries the same operation and is approved on PostgreSQL", async ({
  context,
  page,
  request,
}) => {
  test.setTimeout(120_000);
  await mkdir(evidenceDir, { recursive: true });
  const pageErrors: string[] = [];
  page.on("pageerror", (event) => pageErrors.push(event.message));

  await page.goto("/");
  await page.evaluate(async () => {
    if ("serviceWorker" in navigator) await navigator.serviceWorker.ready;
  });
  await page.reload();
  await browserLogin(page, "stock");
  await openStocktake(page);
  await expect(page.getByLabel("Lô hàng")).toHaveValue(/.+/);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);

  await context.setOffline(true);
  await expect(page.getByText("Ngoại tuyến", { exact: true })).toBeVisible();
  await page.getByLabel("Số lượng thực tế").fill("98");
  await page.getByLabel("Ghi chú", { exact: true }).fill("QA-03 offline mobile");
  await page.getByRole("button", { name: "Lưu phiếu offline" }).click();
  const queued = page.locator(".count-row").first();
  await expect(queued).toContainText("Chờ kết nối");
  const operationId = (await queued.locator(".operation-id").textContent())?.match(
    /[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/i,
  )?.[0];
  expect(operationId).toBeTruthy();
  await page.screenshot({
    path: path.join(evidenceDir, "mobile-offline-390x844.png"),
    fullPage: true,
  });

  await page.reload();
  await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeVisible();
  await context.setOffline(false);

  let committedLine: Record<string, unknown> | undefined;
  let firstResponseDropped = false;
  const loseFirstResponse = async (route: Route) => {
    if (route.request().method() !== "POST" || committedLine) {
      await route.continue();
      return;
    }
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    committedLine = (await response.json()) as Record<string, unknown>;
    await route.abort("timedout");
    firstResponseDropped = true;
  };
  await page.route("**/api/v1/inventory/stocktakes/*/counts", loseFirstResponse);
  await browserLogin(page, "stock");
  await openStocktake(page);
  const retained = page.locator(".count-row").filter({ hasText: operationId! });
  await expect(retained).toContainText("Chờ kết nối");
  await expect.poll(() => firstResponseDropped).toBe(true);
  await page.unroute("**/api/v1/inventory/stocktakes/*/counts", loseFirstResponse);

  await page.getByRole("button", { name: "Đồng bộ 1 phiếu" }).click();
  await expect(page.locator(".count-row").filter({ hasText: operationId! })).toContainText(
    "Chờ duyệt",
  );
  expect(committedLine?.clientOperationId).toBe(operationId);

  const manager = await apiLogin(request, "manager");
  const stocktakeId = String(committedLine?.stocktakeId);
  const approval = await request.post(`/api/v1/inventory/stocktakes/${stocktakeId}/approve`, {
    headers: headers(manager),
  });
  expect(approval.status()).toBe(200);

  const stock = await apiLogin(request, "stock");
  const batchesAfterApproval = await pageItems(
    request,
    "/api/v1/inventory/batches?page=0&size=100",
    stock,
  );
  const approvedBatch = batchesAfterApproval.find(
    (item) => item.batchId === committedLine?.batchId,
  );
  expect(approvedBatch).toMatchObject({ onHandQuantity: "98.000", version: 1 });
  const movementsAfterApproval = await pageItems(
    request,
    "/api/v1/inventory/movements?page=0&size=100",
    stock,
  );
  expect(
    movementsAfterApproval.filter(
      (item) =>
        item.type === "STOCKTAKE_ADJUSTMENT" &&
        (item.source as Record<string, unknown>).id === stocktakeId,
    ),
  ).toHaveLength(1);

  await page.setViewportSize({ width: 1366, height: 768 });
  await page
    .locator("nav")
    .getByRole("button", { name: "Tồn kho & lô hàng", exact: false })
    .click();
  await expect(
    page.locator("tr").filter({ hasText: String(approvedBatch?.batchNumber) }),
  ).toContainText("98");
  await expect(
    page.locator(".movement-entry").filter({ hasText: "Điều chỉnh kiểm kê" }),
  ).toContainText("Phiếu kiểm kê");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({
    path: path.join(evidenceDir, "desktop-approved-1366x768.png"),
    fullPage: true,
  });

  expect(pageErrors).toEqual([]);
});

test("permission and stale snapshot are rejected without a stocktake adjustment", async ({
  request,
}) => {
  const sales = await apiLogin(request, "sales");
  const forbidden = await request.post("/api/v1/inventory/stocktakes", {
    headers: headers(sales),
  });
  expect(forbidden.status()).toBe(403);

  const stock = await apiLogin(request, "stock");
  const manager = await apiLogin(request, "manager");
  const batches = await pageItems(request, "/api/v1/inventory/batches?page=0&size=100", stock);
  const apple = batches.find((item) => item.batchNumber === "BATCH-APPLE-001");
  expect(apple).toBeTruthy();
  const sessionResponse = await request.post("/api/v1/inventory/stocktakes", {
    headers: headers(stock),
  });
  expect(sessionResponse.status()).toBe(201);
  const session = (await sessionResponse.json()) as { id: string };

  const checkout = await request.post("/api/v1/sales/checkout", {
    headers: headers(manager),
    data: {
      storeId,
      items: [{ productId: apple!.productId, quantity: 1 }],
      cashAmount: 100_000,
    },
  });
  expect(checkout.status()).toBe(201);

  const clientOperationId = crypto.randomUUID();
  const conflict = await request.post(`/api/v1/inventory/stocktakes/${session.id}/counts`, {
    headers: headers(stock),
    data: {
      clientOperationId,
      batchId: apple!.batchId,
      actualQuantity: 48,
      baseVersion: apple!.version,
      note: "QA-03 stale snapshot",
      countedAt: new Date().toISOString(),
    },
  });
  expect(conflict.status()).toBe(409);
  expect(await conflict.json()).toMatchObject({
    code: "CONFLICT",
    clientOperationId,
    currentStatus: "CONFLICT",
  });

  const movements = await pageItems(request, "/api/v1/inventory/movements?page=0&size=100", stock);
  expect(
    movements.filter(
      (item) =>
        item.type === "STOCKTAKE_ADJUSTMENT" &&
        (item.source as Record<string, unknown>).id === session.id,
    ),
  ).toHaveLength(0);
  expect(organizationId).toBe(stock.session.organizationId);
});
