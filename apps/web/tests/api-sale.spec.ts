/**
 * Consumer tests for S2-GAP-05 — Sales API adapter (quote, checkout, invoices).
 *
 * Uses Playwright's route mocking to isolate the web consumer from the backend.
 * Follows the pattern established by api-inventory.spec.ts and api-promotions.spec.ts.
 *
 * Scenarios:
 *   1. SALES user sees Bán hàng + Hóa đơn routes after login
 *   2. Full happy path: quote → checkout CASH → navigate to invoices and see invoice row
 *   3. Quote network error shows message to user
 *   4. PRICE_CHANGED error during checkout is shown clearly to user
 *   5. INSUFFICIENT_STOCK error during checkout is shown clearly to user
 *   6. Checkout response loss retries the same payload and Idempotency-Key
 *   7. HTTP 502 after checkout commit retries the same payload and Idempotency-Key
 *   8. FORBIDDEN (403) on listInvoices shows error state with retry button
 *   9. MANAGER/ADMIN sees full store invoice list (sales.read.store permission)
 *  10. SALES user cannot see Bán hàng or Hóa đơn without sales.* permissions
 */

import { expect, test, type Page, type Route } from "@playwright/test";

// ─── Shared fixtures ────────────────────────────────────────────────────────

const token = "s".repeat(43);
const orgId = "10000000-0000-0000-0000-000000000001";
const storeId = "10000000-0000-0000-0000-000000000002";
const salesUserId = "40000000-0000-0000-0000-000000000010";
const productId = "10000000-0000-0000-0000-000000000041";

const salesSession = {
  userId: salesUserId,
  organizationId: orgId,
  storeId,
  fullName: "Lan Nguyễn",
  trainingEnabled: false,
  roles: ["SALES"],
  permissions: ["catalog.read", "sales.write", "sales.create", "sales.read.own"],
};

const managerSession = {
  userId: "40000000-0000-0000-0000-000000000020",
  organizationId: orgId,
  storeId,
  fullName: "An Phạm",
  trainingEnabled: false,
  roles: ["MANAGER"],
  permissions: [
    "catalog.read",
    "inventory.read",
    "sales.write",
    "sales.create",
    "sales.read.store",
    "reports.read",
    "promotions.read",
  ],
};

const stockSession = {
  userId: "40000000-0000-0000-0000-000000000030",
  organizationId: orgId,
  storeId,
  fullName: "Minh Trần",
  trainingEnabled: false,
  roles: ["STOCK"],
  // No sales.* permissions
  permissions: ["catalog.read", "receipts.read", "receipts.write", "inventory.read"],
};

const sampleProduct = {
  id: productId,
  sku: "GAO-001",
  name: "Gạo thơm 5kg",
  status: "ACTIVE",
  baseUnitId: "10000000-0000-0000-0000-000000000032",
};

const sampleQuote = {
  storeId,
  lines: [
    {
      productId,
      productName: "Gạo thơm 5kg",
      quantity: "1",
      unitPrice: 22000,
      grossAmount: 22000,
      discountAmount: 0,
      lineTotal: 22000,
    },
  ],
  subtotal: 22000,
  discountTotal: 0,
  grandTotal: 22000,
};

const sampleInvoice = {
  id: "80000000-0000-0000-0000-000000000001",
  invoiceNo: "INV-ABCD1234",
  status: "COMPLETED",
  soldAt: new Date().toISOString(),
  soldBy: salesUserId,
  subtotal: 22000,
  discountTotal: 0,
  grandTotal: 22000,
  paidTotal: 22000,
  changeAmount: 8000,
  lines: [
    {
      productId,
      sku: "GAO-001",
      productName: "Gạo thơm 5kg",
      quantity: "1",
      unitPrice: 22000,
      discountAmount: 0,
      lineTotal: 22000,
    },
  ],
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

async function json(route: Route, status: number, body?: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

function grant(session = salesSession) {
  return {
    accessToken: token,
    tokenType: "Bearer",
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    session,
  };
}

async function installLogin(page: Page, session = salesSession) {
  await page.route("**/api/v1/auth/login", (route) => json(route, 200, grant(session)));
}

async function installCatalog(page: Page) {
  let version = 0;
  let quantity = 0;
  await page.route("**/api/v1/sales/cart", (route) =>
    json(route, 200, {
      id: "test-shared-cart",
      version,
      generation: 0,
      items: quantity
        ? [{ productId, productName: sampleProduct.name, quantity: String(quantity) }]
        : [],
    }),
  );
  await page.route("**/api/v1/sales/cart/operations", (route) => {
    quantity = Math.max(0, quantity + Number(route.request().postDataJSON().delta));
    version++;
    return json(route, 200, {
      id: "test-shared-cart",
      version,
      generation: 0,
      items: quantity
        ? [{ productId, productName: sampleProduct.name, quantity: String(quantity) }]
        : [],
    });
  });
  await page.route("**/api/v1/products", (route) => json(route, 200, [sampleProduct]));
  await page.route("**/api/v1/categories", (route) => json(route, 200, []));
  await page.route("**/api/v1/units", (route) =>
    json(route, 200, [
      { id: sampleProduct.baseUnitId, code: "KG", name: "Kilôgam", precisionScale: 3 },
    ]),
  );
  await page.route("**/api/v1/suppliers", (route) => json(route, 200, []));
  return {
    setQuantity(value: number) {
      quantity = value;
      version++;
    },
  };
}

async function signIn(page: Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("sales");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openSale(page: Page) {
  await page
    .locator("nav")
    .getByRole("button", { name: /Bán hàng/ })
    .click();
}

test("API sale finds a product by barcode on Enter", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);
  await page.route("**/api/v1/products/search?barcode=8930000000001", (route) =>
    json(route, 200, [sampleProduct]),
  );
  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  await signIn(page);
  await openSale(page);
  const search = page.getByRole("textbox", { name: "Tìm sản phẩm bán hàng" });
  await search.fill("8930000000001");
  await search.press("Enter");
  await expect(page.locator(".cart")).toContainText(sampleProduct.name);
});

test("API catalog shows the server unit without invented price or stock", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);
  await signIn(page);
  await page
    .locator("nav")
    .getByRole("button", { name: /Sản phẩm/ })
    .click();
  const row = page.locator("tbody tr").filter({ hasText: sampleProduct.name });
  await expect(row).toContainText("KG");
  await expect(row).toContainText("Theo báo giá");
  await expect(row).not.toContainText("0 ₫");
  await expect(row.locator(".status")).toHaveText("—");
});

test("sample catalog loads ten local product photos in sale and catalog views", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);
  const skus = [
    "RICE",
    "APPLE",
    "MILK",
    "COOKIE",
    "NOODLE",
    "OIL",
    "SUGAR",
    "SALT",
    "WATER",
    "COFFEE",
  ];
  const products = skus.map((sku, index) => ({
    ...sampleProduct,
    id: `10000000-0000-0000-0000-${String(41 + index).padStart(12, "0")}`,
    sku: `ST-${sku}-01`,
    name: `Mặt hàng ${sku}`,
  }));
  await page.route("**/api/v1/products", (route) => json(route, 200, products));
  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-grid img")).toHaveCount(10);
  for (const product of products) {
    const image = page.locator(".product-grid").getByAltText(product.name, { exact: true });
    await image.scrollIntoViewIfNeeded();
    await expect(image).toHaveJSProperty("naturalWidth", 480);
    expect(new URL((await image.getAttribute("src")) ?? "", page.url()).origin).toBe(
      new URL(page.url()).origin,
    );
  }
  await page
    .locator("nav")
    .getByRole("button", { name: /Sản phẩm/ })
    .click();
  await expect(page.locator(".product-cell img")).toHaveCount(10);
  await expect(page.locator(".product-cell img").first()).toHaveJSProperty("naturalWidth", 480);
});

test("a failed product photo preserves the product name and add action", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);
  await page.route("**/api/v1/products", (route) =>
    json(route, 200, [{ ...sampleProduct, sku: "ST-RICE-01" }]),
  );
  await page.route("**/product-images/rice.webp", (route) => route.abort());
  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  await signIn(page);
  await openSale(page);
  const card = page.locator(".product-card").first();
  await expect(card.locator(".product-image-fallback")).toBeVisible();
  await expect(card).toContainText(sampleProduct.name);
  await card.click();
  await expect(page.locator(".cart-row .stepper span")).toHaveText("1");
});

// ─── Tests ────────────────────────────────────────────────────────────────────

test("SALES user with sales.write sees Bán hàng and Hóa đơn navigation routes", async ({
  page,
}) => {
  await installLogin(page);
  await signIn(page);
  const nav = page.locator("nav");
  await expect(nav.getByRole("button", { name: /Bán hàng/ })).toBeVisible();
  await expect(nav.getByRole("button", { name: /Hóa đơn/ })).toBeVisible();
});

test("STOCK user without sales.* permissions does not see Bán hàng or Hóa đơn", async ({
  page,
}) => {
  await installLogin(page, stockSession);
  await signIn(page);
  const nav = page.locator("nav");
  await expect(nav.getByRole("button", { name: /Bán hàng/ })).toHaveCount(0);
  await expect(nav.getByRole("button", { name: /Hóa đơn/ })).toHaveCount(0);
});

test("happy path: quote → CASH checkout → invoice list shows new invoice row", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);

  const quoteCalls: string[] = [];
  const checkoutCalls: string[] = [];
  await page.route("**/api/v1/sales/quote", async (route) => {
    expect(route.request().method()).toBe("POST");
    expect(route.request().headers()["x-organization-id"]).toBe(orgId);
    expect(route.request().headers()["authorization"]).toBe(`Bearer ${token}`);
    quoteCalls.push(route.request().url());
    await json(route, 200, sampleQuote);
  });
  await page.route("**/api/v1/sales/checkout", async (route) => {
    expect(route.request().method()).toBe("POST");
    const body = JSON.parse(route.request().postData() || "{}");
    expect(body).toMatchObject({ expectedTotal: 22000, cashAmount: 30000 });
    // Idempotency-Key header must be a UUID
    expect(route.request().headers()["idempotency-key"]).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
    checkoutCalls.push(route.request().url());
    await json(route, 201, sampleInvoice);
  });
  await page.route("**/api/v1/sales/invoices", async (route) => {
    if (route.request().method() === "GET") {
      await json(route, 200, [sampleInvoice]);
    }
  });

  await signIn(page);
  await openSale(page);

  // Product card should be visible
  await expect(page.locator(".product-card").first()).toBeVisible();
  await expect(page.locator(".product-card").first()).toContainText(
    "Giá và tồn do máy chủ xác nhận",
  );
  await expect(page.locator(".product-card").first()).not.toContainText("Còn 0 Cái");
  await page.locator(".product-card").first().click();

  // The cashier sees the server quote before confirming payment.
  await expect(page.locator(".cart-row small")).toContainText("22.000");
  await expect(page.locator(".total b")).toContainText("22.000");
  await page.getByLabel("Khách đưa").fill("30000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();

  // Both quote and checkout must have been called
  await expect.poll(() => quoteCalls.length).toBeGreaterThan(0);
  await expect.poll(() => checkoutCalls.length).toBeGreaterThan(0);

  // After checkout, navigate to invoices route; invoice-row must appear
  await expect(page.locator(".invoice-row").first()).toBeVisible();
  await expect(page.locator(".invoice-row").first()).toContainText("INV-ABCD1234");
  await page.locator(".invoice-row").first().click();
  await expect(page.getByRole("dialog")).toContainText("Gạo thơm 5kg × 1");
});

test("quote network error shows user-friendly message", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);

  await page.route("**/api/v1/sales/quote", async (route) => {
    await route.abort("failed");
  });
  let checkoutCalls = 0;
  await page.route("**/api/v1/sales/checkout", async (route) => {
    checkoutCalls++;
    await json(route, 201, sampleInvoice);
  });

  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();

  // Error appears before checkout and an unconfirmed quote cannot be paid.
  await expect(
    page
      .locator("[role='alert'], .toast")
      .filter({ hasText: /mạng|kết nối/i })
      .first(),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: /Xác nhận thanh toán/ })).toBeDisabled();
  expect(checkoutCalls).toBe(0);
});

test("PRICE_CHANGED during checkout shows specific error to user", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);

  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  await page.route("**/api/v1/sales/checkout", async (route) => {
    await json(route, 409, {
      code: "PRICE_CHANGED",
      message: "Giá sản phẩm đã thay đổi. Tổng mới: 25000, tổng kỳ vọng: 22000",
    });
  });

  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("30000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();

  // Must show price-changed specific message
  await expect(
    page
      .locator("[role='alert'], .toast")
      .filter({ hasText: /giá|thay đổi/i })
      .first(),
  ).toBeVisible();
});

test("INSUFFICIENT_STOCK during checkout shows specific error to user", async ({ page }) => {
  await installLogin(page);
  await installCatalog(page);

  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  await page.route("**/api/v1/sales/checkout", async (route) => {
    await json(route, 409, {
      code: "INSUFFICIENT_STOCK",
      message: "Không đủ tồn kho cho sản phẩm GAO-001",
    });
  });

  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("30000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();

  await expect(
    page
      .locator("[role='alert'], .toast")
      .filter({ hasText: /tồn kho|điều chỉnh/i })
      .first(),
  ).toBeVisible();
});

test("checkout response loss retries the exact same payload and Idempotency-Key", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);

  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  const checkoutCalls: Array<{ key: string | undefined; body: string | null }> = [];
  await page.route("**/api/v1/sales/checkout", async (route) => {
    checkoutCalls.push({
      key: route.request().headers()["idempotency-key"],
      body: route.request().postData(),
    });
    if (checkoutCalls.length === 1) {
      // Simulate a committed POST whose response is lost.
      await route.abort("timedout");
    } else {
      await json(route, 200, sampleInvoice);
    }
  });
  await page.route("**/api/v1/sales/invoices", (route) => json(route, 200, [sampleInvoice]));

  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("30000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();

  await expect(page.getByText(/lần thử tiếp theo sẽ dùng đúng mã giao dịch/i)).toBeVisible();
  await expect(page.getByLabel("Khách đưa")).toBeDisabled();
  await page.getByRole("button", { name: /Thử lại cùng mã giao dịch/ }).click();

  await expect.poll(() => checkoutCalls.length).toBe(2);
  expect(checkoutCalls[0].key).toMatch(/^[0-9a-f-]{36}$/i);
  expect(checkoutCalls[1].key).toBe(checkoutCalls[0].key);
  expect(checkoutCalls[1].body).toBe(checkoutCalls[0].body);
  await expect(page.locator(".invoice-row").first()).toContainText("INV-ABCD1234");
});

test("HTTP 502 after checkout commit retries the exact same payload and Idempotency-Key", async ({
  page,
}) => {
  await installLogin(page);
  await installCatalog(page);

  await page.route("**/api/v1/sales/quote", (route) => json(route, 200, sampleQuote));
  const checkoutCalls: Array<{ key: string | undefined; body: string | null }> = [];
  await page.route("**/api/v1/sales/checkout", async (route) => {
    checkoutCalls.push({
      key: route.request().headers()["idempotency-key"],
      body: route.request().postData(),
    });
    if (checkoutCalls.length === 1) {
      // The upstream committed, but the proxy could not return that success.
      await json(route, 502, {
        code: "TEMPORARILY_UNAVAILABLE",
        message: "Upstream response was lost after commit",
      });
    } else {
      await json(route, 200, sampleInvoice);
    }
  });
  await page.route("**/api/v1/sales/invoices", (route) => json(route, 200, [sampleInvoice]));

  await signIn(page);
  await openSale(page);
  await expect(page.locator(".product-card").first()).toBeVisible();
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("30000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();

  await expect(page.getByText(/502.*chưa chắc chắn/i)).toBeVisible();
  await expect(page.getByText(/lần thử tiếp theo sẽ dùng đúng mã giao dịch/i)).toBeVisible();
  await expect(page.getByLabel("Khách đưa")).toBeDisabled();
  await page.getByRole("button", { name: /Thử lại cùng mã giao dịch/ }).click();

  await expect.poll(() => checkoutCalls.length).toBe(2);
  expect(checkoutCalls[0].key).toMatch(/^[0-9a-f-]{36}$/i);
  expect(checkoutCalls[1].key).toBe(checkoutCalls[0].key);
  expect(checkoutCalls[1].body).toBe(checkoutCalls[0].body);
  await expect(page.locator(".invoice-row").first()).toContainText("INV-ABCD1234");
});

test("FORBIDDEN on listInvoices shows error state with retry button", async ({ page }) => {
  await installLogin(page);

  await page.route("**/api/v1/sales/invoices", async (route) => {
    await json(route, 403, { code: "FORBIDDEN", message: "Không có quyền xem hóa đơn" });
  });

  await signIn(page);
  await page
    .locator("nav")
    .getByRole("button", { name: /Hóa đơn/ })
    .click();

  // Error state must appear with retry
  await expect(page.locator("button", { hasText: /Thử lại/ })).toBeVisible();
});

test("MANAGER with sales.read.store sees Bán hàng, Hóa đơn and Báo cáo routes", async ({
  page,
}) => {
  await installLogin(page, managerSession);
  await signIn(page);
  const nav = page.locator("nav");
  await expect(nav.getByRole("button", { name: /Bán hàng/ })).toBeVisible();
  await expect(nav.getByRole("button", { name: /Hóa đơn/ })).toBeVisible();
  await expect(nav.getByRole("button", { name: /Báo cáo/ })).toBeVisible();
});

test("invoices list shows multiple invoices returned by server", async ({ page }) => {
  const invoices = [
    sampleInvoice,
    { ...sampleInvoice, id: "80000000-0000-0000-0000-000000000002", invoiceNo: "INV-BBBB2222" },
  ];
  await installLogin(page);
  await page.route("**/api/v1/sales/invoices", (route) =>
    route.request().method() === "GET" ? json(route, 200, invoices) : route.continue(),
  );

  await signIn(page);
  await page
    .locator("nav")
    .getByRole("button", { name: /Hóa đơn/ })
    .click();

  await expect(page.locator(".invoice-row")).toHaveCount(2);
  await expect(page.locator(".invoice-row").first()).toContainText("INV-ABCD1234");
  await expect(page.locator(".invoice-row").nth(1)).toContainText("INV-BBBB2222");
});

test("CART_CHANGED restores a newer snapshot received while checkout was pending", async ({
  page,
}) => {
  await installLogin(page);
  const remote = await installCatalog(page);
  await page.route("**/api/v1/sales/quote", (route) => {
    const quantity = Number(route.request().postDataJSON().items[0].quantity);
    return json(route, 200, { ...sampleQuote, grandTotal: 22000 * quantity });
  });
  let release!: () => void;
  const hold = new Promise<void>((resolve) => {
    release = resolve;
  });
  let checkoutStarted = false;
  await page.route("**/api/v1/sales/checkout", async (route) => {
    checkoutStarted = true;
    await hold;
    await json(route, 409, { code: "CART_CHANGED", message: "Cart changed" });
  });
  await signIn(page);
  await openSale(page);
  await page.locator(".product-card").first().click();
  await page.getByLabel("Khách đưa").fill("1000000");
  await page.getByRole("button", { name: /Xác nhận thanh toán/ }).click();
  await expect.poll(() => checkoutStarted).toBe(true);
  const changedRead = page.waitForResponse(
    async (response) =>
      new URL(response.url()).pathname === "/api/v1/sales/cart" &&
      response.ok() &&
      (await response.json()).version === 2,
  );
  remote.setQuantity(2);
  await changedRead;
  release();
  await expect(page.locator(".cart-row .stepper span")).toHaveText("2");
  await expect(page.locator(".toast")).toContainText("Giỏ đã thay đổi");
  await expect(page.getByRole("button", { name: /Xác nhận thanh toán/ })).toBeEnabled();
});
