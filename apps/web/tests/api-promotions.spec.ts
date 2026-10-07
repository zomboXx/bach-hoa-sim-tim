import { expect, test, type Route } from "@playwright/test";

const orgId = "10000000-0000-0000-0000-000000000001";
const storeId = "10000000-0000-0000-0000-000000000002";
const productId = "10000000-0000-0000-0000-000000000041";
const batchId = "10000000-0000-0000-0000-0000000000a1";
const promotionId = "10000000-0000-0000-0000-0000000000f1";

async function json(route: Route, status: number, body?: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

test("manager creates, views and edits a scoped promotion through the API", async ({ page }) => {
  await page.route("**/api/v1/auth/login", (route) =>
    json(route, 200, {
      accessToken: "p".repeat(43),
      tokenType: "Bearer",
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      session: {
        userId: "10000000-0000-0000-0000-000000000003",
        organizationId: orgId,
        storeId,
        fullName: "Quản lý",
        trainingEnabled: false,
        roles: ["MANAGER"],
        permissions: ["promotions.read", "promotions.write", "catalog.read", "inventory.read"],
      },
    }),
  );
  await page.route("**/api/v1/products", (route) =>
    json(route, 200, [{ id: productId, sku: "GAO-001", name: "Gạo thơm", status: "ACTIVE" }]),
  );
  await page.route("**/api/v1/inventory/batches?*", (route) =>
    json(route, 200, {
      items: [{ batchId, productId, batchNumber: "LO-01" }],
      totalPages: 1,
    }),
  );

  let promotion: Record<string, unknown> | undefined;
  const calls: string[] = [];
  await page.route("**/api/v1/sales/promotions**", async (route) => {
    const request = route.request();
    expect(request.headers()["x-organization-id"]).toBe(orgId);
    expect(request.headers().authorization).toBe(`Bearer ${"p".repeat(43)}`);
    const segments = new URL(request.url()).pathname.split("/");
    const tail = segments.slice(5);
    const method = request.method();
    calls.push(`${method} ${tail.join("/")}`);
    if (!tail.length && method === "GET") return json(route, 200, promotion ? [promotion] : []);
    if (!tail.length && method === "POST") {
      const body = request.postDataJSON();
      expect(body.status).toBe("DRAFT");
      promotion = {
        id: promotionId,
        organizationId: orgId,
        storeId,
        ...body,
        productIds: [],
        batchIds: [],
      };
      return json(route, 201, promotion);
    }
    if (tail[0] === promotionId && tail.length === 1 && method === "GET")
      return json(route, 200, promotion);
    if (tail[0] === promotionId && tail.length === 1 && method === "PUT") {
      promotion = { ...promotion, ...request.postDataJSON() };
      return json(route, 200, promotion);
    }
    if (tail[0] === promotionId && tail[1] === "products" && method === "POST") {
      promotion = { ...promotion, productIds: [request.postDataJSON().productId] };
      return json(route, 201);
    }
    if (tail[0] === promotionId && tail[1] === "products" && method === "DELETE") {
      promotion = { ...promotion, productIds: [] };
      return json(route, 204);
    }
    return json(route, 404);
  });

  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("manager");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("password");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: /Khuyến mãi/ })
    .click();
  await page.getByRole("button", { name: /Tạo khuyến mãi/ }).click();
  await page.getByLabel("Mã", { exact: true }).fill("GAO10");
  await page.getByLabel("Tên", { exact: true }).fill("Giảm gạo 10%");
  await page.getByLabel("Trạng thái").selectOption("ACTIVE");
  await page.getByRole("combobox", { name: "Phạm vi" }).selectOption("PRODUCT");
  await page.getByRole("combobox", { name: "Chọn sản phẩm" }).selectOption(productId);
  await page.getByRole("button", { name: "Lưu khuyến mãi" }).click();

  await expect(page.getByRole("heading", { name: "Giảm gạo 10%" })).toBeVisible();
  await expect(page.getByText("Sản phẩm: Gạo thơm")).toBeVisible();
  expect(calls).toContain(`POST ${promotionId}/products`);
  expect(promotion?.status).toBe("ACTIVE");

  await page.getByRole("button", { name: "Sửa chương trình" }).click();
  await page.getByLabel("Tên", { exact: true }).fill("Giảm gạo mới");
  await page.getByRole("button", { name: "Lưu khuyến mãi" }).click();
  await expect(page.getByRole("heading", { name: "Giảm gạo mới" })).toBeVisible();
  expect(promotion?.name).toBe("Giảm gạo mới");
});
