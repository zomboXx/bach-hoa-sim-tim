import { expect, test, type Page, type Route } from "@playwright/test";

const token = "i".repeat(43);
const supplierId = "10000000-0000-0000-0000-000000000061";
const productId = "10000000-0000-0000-0000-000000000041";
const batchId = "50000000-0000-0000-0000-000000000001";
const userId = "40000000-0000-0000-0000-000000000001";

const stockSession = {
  userId,
  organizationId: "10000000-0000-0000-0000-000000000001",
  storeId: "10000000-0000-0000-0000-000000000002",
  fullName: "Nhân viên kho",
  trainingEnabled: false,
  roles: ["STOCK"],
  permissions: ["catalog.read", "receipts.read", "receipts.write", "inventory.read"],
};

const supplier = {
  id: supplierId,
  organizationId: stockSession.organizationId,
  code: "NCC-AN-NHIEN",
  name: "Phân phối An Nhiên",
  phone: "0901234567",
  email: null,
  status: "ACTIVE",
};

const product = {
  id: productId,
  organizationId: stockSession.organizationId,
  categoryId: "20000000-0000-0000-0000-000000000001",
  baseUnitId: "30000000-0000-0000-0000-000000000001",
  sku: "GAO-001",
  name: "Gạo thơm 5kg",
  tracksExpiry: true,
  status: "ACTIVE",
  version: 0,
};

const secondProduct = {
  ...product,
  id: "10000000-0000-0000-0000-000000000042",
  sku: "SUA-002",
  name: "Sữa tươi ít đường",
};

const productPage = {
  items: [
    {
      productId,
      sku: product.sku,
      productName: product.name,
      productStatus: "ACTIVE",
      onHandQuantity: "12.500",
      availableQuantity: "10.000",
      businessDate: "2026-10-03",
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
      expiryStatus: "NEAR_EXPIRY",
      expiryDate: "2026-10-10",
      receivedDate: "2026-10-03",
      onHandQuantity: "12.500",
      availableQuantity: "10.000",
      businessDate: "2026-10-03",
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
};

const movementPage = {
  items: [
    {
      movementId: "60000000-0000-0000-0000-000000000001",
      productId,
      batchId,
      type: "RECEIPT",
      quantityDelta: "12.500",
      source: { type: "GOODS_RECEIPT", id: "70000000-0000-0000-0000-000000000001" },
      occurredAt: "2026-10-03T02:00:00Z",
      recordedBy: userId,
    },
  ],
  page: 0,
  size: 100,
  totalElements: 1,
  totalPages: 1,
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

async function installCatalog(page: Page) {
  await page.route("**/api/v1/suppliers", (route) => json(route, 200, [supplier]));
  await page.route("**/api/v1/products", (route) =>
    json(route, 200, [product, { ...product, id: crypto.randomUUID(), status: "INACTIVE" }]),
  );
}

async function installInventory(page: Page, status = 200) {
  await page.route("**/api/v1/inventory/products?*", (route) =>
    json(
      route,
      status,
      status === 200 ? productPage : { code: "FORBIDDEN", message: "Thiếu quyền" },
    ),
  );
  await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, batchPage));
  await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, movementPage));
}

async function signIn(page: Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openReceive(page: Page) {
  await page
    .locator("nav")
    .getByRole("button", { name: /Nhận hàng/ })
    .click();
}

async function fillValidReceipt(page: Page) {
  await page.getByLabel("Giá nhập (VND)").fill("24500");
  await page.getByLabel(/Số lô nhà cung cấp/).fill("LOT-2026-10");
  await page.getByLabel(/Hạn sử dụng/).fill("2026-12-31");
}

function receiptResponse(clientOperationId: string) {
  return {
    id: "70000000-0000-0000-0000-000000000001",
    organizationId: stockSession.organizationId,
    storeId: stockSession.storeId,
    supplierId,
    status: "CONFIRMED",
    receivedAt: "2026-10-03T02:00:00Z",
    confirmedBy: userId,
    clientOperationId,
    lines: [
      {
        id: "71000000-0000-0000-0000-000000000001",
        productId,
        expectedQuantity: "10",
        deliveredQuantity: "10",
        acceptedQuantity: "10",
        rejectedQuantity: "0",
        unitCost: 24500,
        supplierLotNumber: "LOT-2026-10",
        expiryDate: "2026-12-31",
        discrepancyReason: null,
      },
    ],
  };
}

test("valid receipt uses the real contract, stable key, active catalog and refreshes inventory", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);
  await installInventory(page);
  let inventoryReads = 0;
  await page.route("**/api/v1/inventory/products?*", async (route) => {
    inventoryReads++;
    await json(route, 200, productPage);
  });
  let postedBody: any;
  let postedKey = "";
  let savedReceipt: ReturnType<typeof receiptResponse> | undefined;
  await page.route("**/api/v1/inventory/receipts?*", (route) =>
    json(route, 200, savedReceipt ? [savedReceipt] : []),
  );
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    expect(route.request().headers().authorization).toBe(`Bearer ${token}`);
    expect(route.request().headers()["x-organization-id"]).toBe(stockSession.organizationId);
    expect(route.request().headers()["x-store-id"]).toBe(stockSession.storeId);
    postedKey = route.request().headers()["idempotency-key"];
    postedBody = route.request().postDataJSON();
    savedReceipt = receiptResponse(postedBody.clientOperationId);
    await json(route, 201, savedReceipt);
  });

  await signIn(page);
  await openReceive(page);
  await expect(page.getByRole("option", { name: /NCC-AN-NHIEN/ })).toHaveCount(1);
  await fillValidReceipt(page);
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByRole("status")).toContainText("Tồn kho vừa được đọc lại");

  expect(postedKey).toMatch(/^[0-9a-f-]{36}$/);
  expect(postedBody).toEqual({
    supplierId,
    clientOperationId: expect.stringMatching(/^[0-9a-f-]{36}$/),
    lines: [
      {
        productId,
        expectedQuantity: "10",
        deliveredQuantity: "10",
        acceptedQuantity: "10",
        rejectedQuantity: "0",
        unitCost: 24500,
        supplierLotNumber: "LOT-2026-10",
        expiryDate: "2026-12-31",
        discrepancyReason: null,
      },
    ],
  });
  expect(inventoryReads).toBeGreaterThan(0);
  const storage = await page.evaluate(() => JSON.stringify({ ...localStorage, ...sessionStorage }));
  expect(storage).not.toContain(token);
});

test("client validation and server field errors stay beside their fields", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);
  await page.route("**/api/v1/inventory/receipts?*", (route) => json(route, 200, []));
  let posts = 0;
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    posts++;
    await json(route, 400, {
      code: "INVALID_REQUEST",
      message: "Dữ liệu không hợp lệ",
      fieldErrors: [{ field: "lines[0].unitCost", message: "phải lớn hơn hoặc bằng 0" }],
    });
  });
  await signIn(page);
  await openReceive(page);
  await page.getByRole("textbox", { name: /^Số nhận/ }).fill("8");
  await page.getByRole("textbox", { name: /^Số từ chối/ }).fill("1");
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByText("Số nhận + số từ chối phải bằng số giao.").first()).toBeVisible();
  expect(posts).toBe(0);

  await page.getByRole("textbox", { name: /^Số nhận/ }).fill("9");
  await page.getByRole("textbox", { name: /^Lý do sai lệch/ }).fill("Một gói rách");
  await page.getByLabel("Giá nhập (VND)").fill("9007199254740992");
  await page.getByLabel(/Hạn sử dụng/).fill("2026-12-31");
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByText(/số nguyên VND không âm trong giới hạn an toàn/)).toBeVisible();
  expect(posts).toBe(0);

  await page.getByLabel("Giá nhập (VND)").fill("12000");
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByText("phải lớn hơn hoặc bằng 0")).toBeVisible();
  expect(posts).toBe(1);
});

test("pending POST disables confirmation and ignores a second submit", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);
  await installInventory(page);
  await page.route("**/api/v1/inventory/receipts?*", (route) => json(route, 200, []));
  let release: (() => void) | undefined;
  let posts = 0;
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    posts++;
    await new Promise<void>((resolve) => (release = resolve));
    const body = route.request().postDataJSON();
    await json(route, 201, receiptResponse(body.clientOperationId));
  });
  await signIn(page);
  await openReceive(page);
  await fillValidReceipt(page);
  const submit = page.getByRole("button", { name: "Xác nhận phiếu nhận" });
  await submit.dblclick();
  await expect(page.getByRole("button", { name: "Đang xác nhận…" })).toBeDisabled();
  expect(posts).toBe(1);
  release?.();
  await expect(page.getByRole("status")).toContainText("Tồn kho vừa được đọc lại");
});

for (const status of [401, 403]) {
  test(`inventory HTTP ${status} follows the authentication and permission UX`, async ({
    page,
  }) => {
    await installLogin(page);
    await page.route("**/api/v1/inventory/products?*", (route) =>
      json(route, status, {
        code: status === 401 ? "UNAUTHENTICATED" : "FORBIDDEN",
        message: status === 401 ? "Valid bearer session required" : "Thiếu quyền inventory.read",
      }),
    );
    await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, batchPage));
    await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, movementPage));
    await signIn(page);
    await page
      .locator("nav")
      .getByRole("button", { name: /Tồn kho/ })
      .click();
    if (status === 401) {
      await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeVisible();
      await expect(page.getByRole("alert")).toContainText("hết hạn");
    } else {
      await expect(page.getByRole("alert")).toContainText("Thiếu quyền inventory.read");
      await expect(page.locator("nav")).toBeVisible();
    }
  });
}

test("timeout survives reload; empty recovery GET never resends and manual retry reuses the key", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);
  await installInventory(page);
  let confirmed: ReturnType<typeof receiptResponse> | undefined;
  let posts = 0;
  const keys: string[] = [];
  await page.route("**/api/v1/inventory/receipts?*", (route) =>
    json(route, 200, confirmed ? [confirmed] : []),
  );
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    posts++;
    keys.push(route.request().headers()["idempotency-key"]);
    const body = route.request().postDataJSON();
    if (posts === 1) {
      await new Promise((resolve) => setTimeout(resolve, 800));
      await route.abort("timedout").catch(() => undefined);
      return;
    }
    confirmed = receiptResponse(body.clientOperationId);
    await json(route, 201, confirmed);
  });

  await signIn(page);
  await openReceive(page);
  await fillValidReceipt(page);
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByText("Thao tác trước chưa có kết luận")).toBeVisible();
  expect(posts).toBe(1);

  await page.reload();
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await openReceive(page);
  await expect(page.getByText("Chưa tìm thấy phiếu cho thao tác trước")).toBeVisible();
  expect(posts).toBe(1);

  await page.getByRole("button", { name: "Thử lại cùng mã" }).click();
  await expect(page.getByRole("status")).toContainText("Tồn kho vừa được đọc lại");
  expect(posts).toBe(2);
  expect(keys[1]).toBe(keys[0]);
});

test("a saved receipt followed by HTTP 502 is recovered without a second POST", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);
  await installInventory(page);
  let confirmed: ReturnType<typeof receiptResponse> | undefined;
  let posts = 0;
  await page.route("**/api/v1/inventory/receipts?*", (route) =>
    json(route, 200, confirmed ? [confirmed] : []),
  );
  await page.route("**/api/v1/inventory/receipts", async (route) => {
    posts++;
    const body = route.request().postDataJSON();
    confirmed = receiptResponse(body.clientOperationId);
    await json(route, 502, { code: "BAD_GATEWAY", message: "Upstream mất kết nối" });
  });

  await signIn(page);
  await openReceive(page);
  await fillValidReceipt(page);
  await page.getByRole("button", { name: "Xác nhận phiếu nhận" }).click();
  await expect(page.getByText("Thao tác trước chưa có kết luận")).toBeVisible();
  expect(posts).toBe(1);

  await page.reload();
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await openReceive(page);
  await expect(page.getByRole("status")).toContainText("Máy chủ đã ghi phiếu");
  expect(posts).toBe(1);
});

test("inventory reads every server page instead of silently stopping at 100 rows", async ({
  page,
}) => {
  await installLogin(page);
  await page.route("**/api/v1/inventory/products?*", async (route) => {
    const requestedPage = Number(new URL(route.request().url()).searchParams.get("page"));
    const item = {
      ...productPage.items[0],
      productId: requestedPage === 0 ? productId : secondProduct.id,
      sku: requestedPage === 0 ? product.sku : secondProduct.sku,
      productName: requestedPage === 0 ? product.name : secondProduct.name,
    };
    await json(route, 200, {
      items: [item],
      page: requestedPage,
      size: 100,
      totalElements: 2,
      totalPages: 2,
    });
  });
  const emptyPage = { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 };
  await page.route("**/api/v1/inventory/batches?*", (route) => json(route, 200, emptyPage));
  await page.route("**/api/v1/inventory/movements?*", (route) => json(route, 200, emptyPage));

  await signIn(page);
  await page
    .locator("nav")
    .getByRole("button", { name: /Tồn kho/ })
    .click();
  await expect(page.getByText(product.name)).toBeVisible();
  await expect(page.getByText(secondProduct.name)).toBeVisible();
});

test("recent receipts render every line returned by the server", async ({ page }) => {
  await installLogin(page);
  await page.route("**/api/v1/suppliers", (route) => json(route, 200, [supplier]));
  await page.route("**/api/v1/products", (route) => json(route, 200, [product, secondProduct]));
  await installInventory(page);
  const receipt = receiptResponse("80000000-0000-0000-0000-000000000001");
  receipt.lines.push({
    ...receipt.lines[0],
    id: "71000000-0000-0000-0000-000000000002",
    productId: secondProduct.id,
    acceptedQuantity: "4",
    supplierLotNumber: "LOT-SUA-02",
  });
  await page.route("**/api/v1/inventory/receipts?*", (route) => json(route, 200, [receipt]));

  await signIn(page);
  await openReceive(page);
  await expect(page.locator(".receipt-row").first()).toContainText("2 dòng");
  await expect(page.locator(".receipt-row").first()).toContainText(product.name);
  await expect(page.locator(".receipt-row").first()).toContainText(secondProduct.name);
  await expect(page.locator(".receipt-row").first()).toContainText("LOT-SUA-02");
});

test("SALES reads quantities, expiry and movement source without purchase cost on mobile", async ({
  page,
}) => {
  const sales = {
    ...stockSession,
    fullName: "Nhân viên bán hàng",
    roles: ["SALES"],
    permissions: ["inventory.read"],
  };
  await installLogin(page, sales);
  await installInventory(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await signIn(page);
  await expect(page.locator("nav").getByRole("button", { name: /Nhận hàng/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Mở menu" }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: /Tồn kho/ })
    .click();
  await expect(page.getByText("12.500").first()).toBeVisible();
  await expect(page.getByText("Cận hạn")).toBeVisible();
  await expect(page.getByText(/Phiếu nhận 70000000/)).toBeVisible();
  await expect(page.getByText(/Giá nhập|unitCost/i)).toHaveCount(0);
  const hasPageOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
  );
  expect(hasPageOverflow).toBe(false);
});
