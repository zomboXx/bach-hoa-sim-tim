import { expect, test, type Page, type Route } from "@playwright/test";
import type { CatalogKind } from "../src/modules/catalog/api-catalog-adapter";

const token = "c".repeat(43);
const orgId = "10000000-0000-0000-0000-000000000001";
const storeId = "10000000-0000-0000-0000-000000000002";
const categoryId = "10000000-0000-0000-0000-000000000021";
const unitId = "10000000-0000-0000-0000-000000000031";
const productId = "10000000-0000-0000-0000-000000000041";

type RecordValue = Record<string, unknown>;
const kinds: CatalogKind[] = ["products", "categories", "units", "suppliers"];

function response(route: Route, status: number, body?: unknown) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? "" : JSON.stringify(body),
  });
}

async function setup(page: Page, write = true) {
  const session = {
    userId: "40000000-0000-0000-0000-000000000030",
    organizationId: orgId,
    storeId,
    fullName: "Minh Trần",
    trainingEnabled: false,
    roles: [write ? "STOCK" : "SALES"],
    permissions: write ? ["catalog.read", "catalog.write"] : ["catalog.read"],
  };
  const data: Record<CatalogKind, RecordValue[]> = {
    products: [
      {
        id: productId,
        sku: "RICE-01",
        name: "Gạo thơm",
        categoryId,
        baseUnitId: unitId,
        tracksExpiry: false,
        status: "ACTIVE",
      },
    ],
    categories: [
      { id: categoryId, code: "FOOD", name: "Thực phẩm", parentId: null, status: "ACTIVE" },
    ],
    units: [{ id: unitId, code: "KG", name: "Kilôgam", precisionScale: 3 }],
    suppliers: [],
  };
  const barcodes = new Map<string, RecordValue[]>([[productId, []]]);
  const writes: Array<{ method: string; path: string; body?: RecordValue }> = [];
  let failNextList = false;
  let conflictDelete = false;

  await page.route("**/api/v1/auth/login", (route) =>
    response(route, 200, {
      accessToken: token,
      tokenType: "Bearer",
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      session,
    }),
  );
  await page.route("**/api/v1/{products,categories,units,suppliers}**", async (route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname;
    const parts = path.split("/").filter(Boolean).slice(2);
    const kind = parts[0] as CatalogKind;
    if (!kinds.includes(kind)) return response(route, 404);
    if (
      request.headers()["authorization"] !== `Bearer ${token}` ||
      request.headers()["x-organization-id"] !== orgId
    )
      return response(route, 401, { message: "Missing session" });
    if (failNextList && method === "GET" && parts.length === 1) {
      failNextList = false;
      return response(route, 503, { detail: "Máy chủ tạm thời không phản hồi." });
    }
    if (parts[2] === "barcodes") {
      const product = data.products.find((item) => item.id === parts[1]);
      if (!product) return response(route, 404);
      const list = barcodes.get(parts[1]) || [];
      if (method === "GET") {
        const item = parts[3] ? list.find((value) => value.id === parts[3]) : list;
        return response(route, item ? 200 : 404, item);
      }
      if (!write) return response(route, 403);
      const body = method === "DELETE" ? undefined : (request.postDataJSON() as RecordValue);
      writes.push({ method, path, body });
      if (method === "POST") {
        const item = { ...body, id: crypto.randomUUID() };
        list.push(item);
        barcodes.set(parts[1], list);
        return response(route, 201, item);
      }
      const index = list.findIndex((item) => item.id === parts[3]);
      if (index < 0) return response(route, 404);
      if (method === "PUT") {
        list[index] = { ...body, id: parts[3] };
        return response(route, 200, list[index]);
      }
      list.splice(index, 1);
      return response(route, 204);
    }
    const list = data[kind];
    if (method === "GET") {
      const item = parts[1] ? list.find((value) => value.id === parts[1]) : list;
      return response(route, item ? 200 : 404, item);
    }
    if (!write) return response(route, 403);
    const body = method === "DELETE" ? undefined : (request.postDataJSON() as RecordValue);
    writes.push({ method, path, body });
    if (method === "DELETE") {
      if (conflictDelete) return response(route, 409, { detail: "Dữ liệu đang được tham chiếu." });
      const index = list.findIndex((item) => item.id === parts[1]);
      if (index < 0) return response(route, 404);
      if (kind === "products" && (barcodes.get(parts[1])?.length || 0) > 0)
        return response(route, 409, { detail: "Sản phẩm còn mã vạch." });
      list.splice(index, 1);
      return response(route, 204);
    }
    if (!body) return response(route, 422);
    if (kind === "products" && list.some((item) => item.sku === body.sku && item.id !== parts[1]))
      return response(route, 409, { detail: "SKU đã tồn tại." });
    if (method === "POST") {
      const item = { ...body, id: crypto.randomUUID() };
      list.push(item);
      return response(route, 201, item);
    }
    const index = list.findIndex((item) => item.id === parts[1]);
    if (index < 0) return response(route, 404);
    list[index] = { ...body, id: parts[1] };
    return response(route, 200, list[index]);
  });
  return {
    data,
    barcodes,
    writes,
    failList: () => (failNextList = true),
    conflictDelete: () => (conflictDelete = true),
  };
}

async function signIn(page: Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}

async function openCatalog(page: Page) {
  await page
    .locator("nav")
    .getByRole("button", { name: /Sản phẩm/ })
    .click();
  await expect(page.getByRole("heading", { name: "Quản lý hàng hóa" })).toBeVisible();
}

test("API catalog creates, updates and deletes a product and its barcode", async ({ page }) => {
  const api = await setup(page);
  await signIn(page);
  await openCatalog(page);
  await expect(page.getByRole("row").filter({ hasText: "Gạo thơm" })).toContainText("KG");

  await page.getByRole("button", { name: "Thêm sản phẩm" }).click();
  await page.getByLabel("SKU *").fill("COFFEE-02");
  await page.getByLabel("Tên *").fill("Cà phê rang");
  await page.getByLabel("Danh mục *").selectOption(categoryId);
  await page.getByLabel("Đơn vị tính *").selectOption(unitId);
  await page.getByLabel("Theo dõi hạn sử dụng").check();
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  const row = page.getByRole("row").filter({ hasText: "Cà phê rang" });
  await expect(row).toBeVisible();
  expect(api.data.products.find((item) => item.sku === "COFFEE-02")).toMatchObject({
    tracksExpiry: true,
    categoryId,
    baseUnitId: unitId,
  });

  await row.getByRole("button", { name: "Mã vạch" }).click();
  const panel = page.getByRole("region", { name: "Mã vạch sản phẩm" });
  await panel.getByRole("button", { name: "Thêm mã vạch" }).click();
  await panel.getByLabel("Mã vạch *").fill("001234567890");
  await panel.getByLabel("Mã vạch chính").check();
  await panel.getByRole("button", { name: "Lưu mã vạch" }).click();
  await expect(panel).toContainText("001234567890");
  await panel.getByRole("button", { name: "Sửa" }).click();
  await panel.getByLabel("Mã vạch *").fill("001234567891");
  await panel.getByRole("button", { name: "Lưu mã vạch" }).click();
  await expect(panel).toContainText("001234567891");

  await row.getByRole("button", { name: "Sửa" }).click();
  await page.getByLabel("Tên *").fill("Cà phê rang mới");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("row").filter({ hasText: "Cà phê rang mới" })).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept());
  await panel.getByRole("button", { name: "Xóa" }).click();
  await expect(panel).not.toContainText("001234567891");
  page.once("dialog", (dialog) => dialog.accept());
  await page
    .getByRole("row")
    .filter({ hasText: "Cà phê rang mới" })
    .getByRole("button", { name: "Xóa" })
    .click();
  await expect(page.getByRole("row").filter({ hasText: "Cà phê rang mới" })).toHaveCount(0);
  expect(api.data.products).toHaveLength(1);
  expect(api.writes.map((call) => call.method)).toContain("PUT");

  await page.setViewportSize({ width: 390, height: 844 });
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth))
    .toBeLessThanOrEqual(390);
});

test("API catalog manages categories, units and suppliers with server errors", async ({ page }) => {
  const api = await setup(page);
  await signIn(page);
  await openCatalog(page);
  await page.getByRole("tab", { name: "Danh mục" }).click();
  await page.getByRole("button", { name: "Thêm danh mục" }).click();
  await page.getByLabel("Mã *").fill("DRINKS");
  await page.getByLabel("Tên *").fill("Đồ uống");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  const category = page.getByRole("row").filter({ hasText: "Đồ uống" });
  await expect(category).toBeVisible();
  await category.getByRole("button", { name: "Sửa" }).click();
  await page.getByLabel("Tên *").fill("Đồ uống lạnh");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("row").filter({ hasText: "Đồ uống lạnh" })).toBeVisible();

  await page.getByRole("tab", { name: "Đơn vị tính" }).click();
  await page.getByRole("button", { name: "Thêm đơn vị tính" }).click();
  await page.getByLabel("Mã *").fill("BOX");
  await page.getByLabel("Tên *").fill("Thùng");
  await page.getByLabel("Chữ số thập phân *").fill("0");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  const unit = page.getByRole("row").filter({ hasText: "Thùng" });
  await expect(unit).toBeVisible();
  await unit.getByRole("button", { name: "Sửa" }).click();
  await page.getByLabel("Tên *").fill("Thùng hàng");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("row").filter({ hasText: "Thùng hàng" })).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept());
  await page
    .getByRole("row")
    .filter({ hasText: "Thùng hàng" })
    .getByRole("button", { name: "Xóa" })
    .click();
  await expect(page.getByRole("row").filter({ hasText: "Thùng hàng" })).toHaveCount(0);

  await page
    .locator("nav")
    .getByRole("button", { name: /Nhà cung cấp/ })
    .click();
  await expect(page.getByRole("tab", { name: "Nhà cung cấp" })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await page.getByRole("button", { name: "Thêm nhà cung cấp" }).click();
  await page.getByLabel("Mã *").fill("SUP-02");
  await page.getByLabel("Tên *").fill("Nhà cung cấp mới");
  await page.getByLabel("Email").fill("ncc@example.com");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  const supplier = page.getByRole("row").filter({ hasText: "Nhà cung cấp mới" });
  await expect(supplier).toBeVisible();
  api.conflictDelete();
  page.once("dialog", (dialog) => dialog.accept());
  await supplier.getByRole("button", { name: "Xóa" }).click();
  await expect(page.getByRole("alert")).toContainText("Dữ liệu đang được tham chiếu");
  await expect(supplier).toBeVisible();
  await supplier.getByRole("button", { name: "Sửa" }).click();
  await page.getByLabel("Trạng thái").selectOption("INACTIVE");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(supplier).toContainText("Ngừng hợp tác");
  expect(api.data.suppliers[0].status).toBe("INACTIVE");
});

test("catalog read permission shows data without write controls and recovers from load failure", async ({
  page,
}) => {
  const api = await setup(page, false);
  const dashboardCatalogLoaded = Promise.all(
    ["/api/v1/products", "/api/v1/units"].map((path) =>
      page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname === path && response.request().method() === "GET",
      ),
    ),
  );
  await signIn(page);
  await dashboardCatalogLoaded;
  api.failList();
  await openCatalog(page);
  await expect(page.getByRole("alert")).toContainText("Máy chủ tạm thời không phản hồi");
  await page.getByRole("button", { name: "Tải lại" }).click();
  await expect(page.getByRole("row").filter({ hasText: "Gạo thơm" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Thêm sản phẩm/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Sửa" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Xóa" })).toHaveCount(0);
  await page
    .getByRole("row")
    .filter({ hasText: "Gạo thơm" })
    .getByRole("button", { name: "Mã vạch" })
    .click();
  await expect(page.getByRole("region", { name: "Mã vạch sản phẩm" })).toBeVisible();
  expect(api.writes).toHaveLength(0);
});
