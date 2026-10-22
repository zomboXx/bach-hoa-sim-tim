import { expect, test, type Page } from "@playwright/test";

const session = {
  userId: "40000000-0000-0000-0000-000000000010",
  organizationId: "10000000-0000-0000-0000-000000000001",
  storeId: "10000000-0000-0000-0000-000000000002",
  fullName: "Thu ngân",
  roles: ["SALES"],
  trainingEnabled: false,
  permissions: ["catalog.read", "sales.write", "sales.create", "sales.read.own"],
};
const productId = "10000000-0000-0000-0000-000000000041";

async function fixture(page: Page, enabled: boolean) {
  let quantity = 0;
  let version = 0;
  let pendingId: string | null = null;
  let status = "PENDING";
  const creations: unknown[] = [];
  const order = () => ({
    id: "80000000-0000-0000-0000-000000000001",
    invoiceId: "80000000-0000-0000-0000-000000000002",
    ownerId: session.userId,
    reference: "Chuyen tien",
    amount: 1000,
    status,
    bankBin: "970416",
    accountNumber: "257678859",
    accountName: "TEST ONLY",
    createdAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + 600000).toISOString(),
    resolvedAt: null,
    resolvedBy: null,
    bankReference: null,
    resolutionNote: null,
    qrPayload:
      status === "PENDING"
        ? "00020101021238530010A0000007270123000697041601092576788590208QRIBFTTA5303704540410005802VN62150811Chuyen tien6304BBB8"
        : null,
  });
  const cart = () => ({
    id: "cart-test",
    version,
    generation: 0,
    pendingTransferId: pendingId,
    items: quantity ? [{ productId, productName: "Hàng thử", quantity: String(quantity) }] : [],
  });
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    let body: unknown = [];
    if (path.endsWith("/auth/login") || path.endsWith("/auth/restore"))
      body = {
        accessToken: "a".repeat(43),
        tokenType: "Bearer",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        session,
      };
    else if (path.endsWith("/sales/cart")) body = cart();
    else if (path.endsWith("/cart/operations")) {
      quantity++;
      version++;
      body = cart();
    } else if (path.endsWith("/products"))
      body = [
        {
          id: productId,
          sku: "TEST",
          name: "Hàng thử",
          status: "ACTIVE",
          baseUnitId: "unit-test",
          salePrice: 1000,
        },
      ];
    else if (path.endsWith("/units"))
      body = [{ id: "unit-test", code: "CAI", name: "Cái", precisionScale: 0 }];
    else if (path.endsWith("/sales/quote"))
      body = {
        storeId: session.storeId,
        subtotal: 1000,
        discountTotal: 0,
        grandTotal: 1000,
        lines: [
          {
            productId,
            productName: "Hàng thử",
            quantity: "1",
            unitPrice: 1000,
            grossAmount: 1000,
            discountAmount: 0,
            lineTotal: 1000,
          },
        ],
      };
    else if (path.endsWith("/transfers/config")) body = { enabled, expiresInMinutes: 10 };
    else if (path.endsWith("/sales/transfers") && route.request().method() === "POST") {
      creations.push(route.request().postDataJSON());
      if (creations.length === 1)
        return route.fulfill({
          status: 502,
          contentType: "application/json",
          body: '{"message":"Chưa rõ kết quả tạo QR"}',
        });
      pendingId = order().id;
      version++;
      body = order();
    } else if (path.endsWith("/sales/transfers"))
      body = {
        items: pendingId || status === "CANCELLED" ? [order()] : [],
        summary: {
          pending: pendingId ? 1 : 0,
          completed: 0,
          cancelled: status === "CANCELLED" ? 1 : 0,
          expired: 0,
          receivedAmount: 0,
        },
        page: 0,
        size: 10,
      };
    else if (path.endsWith("/cancel")) {
      status = "CANCELLED";
      pendingId = null;
      version++;
      body = order();
    } else if (path.includes("/transfers/")) body = order();
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
  return creations;
}
async function login(page: Page) {
  await page.goto("/");
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("sales");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("test-pass");
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: /Bán hàng/ })
    .click();
}

test("API transfer preserves its creation operation across reload and unlocks cart after cancellation", async ({
  page,
}) => {
  const creations = await fixture(page, true);
  await login(page);
  await page.locator(".product-card").click();
  await expect(page.locator(".total b")).toContainText("1.000");
  await page.getByLabel("Phương thức").selectOption("Chuyển khoản QR");
  await page.getByRole("button", { name: "Tạo QR và giữ hàng" }).click();
  await expect(page.getByRole("button", { name: "Thử lại tạo QR cùng mã" })).toBeEnabled();
  await page.reload();
  await page
    .locator("nav")
    .getByRole("button", { name: /Bán hàng/ })
    .click();
  await expect(page.getByLabel("Phương thức")).toBeDisabled();
  await expect(page.getByRole("button", { name: "Tăng Hàng thử" })).toBeDisabled();
  await page.getByRole("button", { name: "Thử lại tạo QR cùng mã" }).click();
  await expect(page.locator(".transfer-detail h3")).toHaveText("Chờ xác nhận tiền về");
  await expect(page.getByLabel("Phương thức")).toHaveValue("Chuyển khoản QR");
  await expect(page.getByLabel("Khách đưa")).toHaveCount(0);
  expect(creations).toHaveLength(2);
  expect(creations[0]).toEqual(creations[1]);
  await expect(page.locator(".transfer-qr svg")).toBeVisible();
  await page.getByText("Hủy giao dịch chờ", { exact: true }).click();
  await page.getByLabel("Lý do hủy").fill("Khách đổi ý, chưa nhận tiền");
  await page.getByRole("button", { name: "Hủy QR và trả hàng giữ" }).click();
  await expect(page.locator(".transfer-detail h3")).toHaveText("Đã hủy");
  await expect(page.getByRole("button", { name: "Tăng Hàng thử" })).toBeEnabled();
  await expect(page.locator(".cart-row .stepper span")).toHaveText("1");
  await expect(page.locator(".transfer-qr svg")).toHaveCount(0);
});

test("API transfer stays disabled without beneficiary configuration and cash remains available", async ({
  page,
}) => {
  await fixture(page, false);
  await login(page);
  await page.locator(".product-card").click();
  await page.getByLabel("Phương thức").selectOption("Chuyển khoản QR");
  await expect(page.getByText(/Cửa hàng chưa cấu hình tài khoản/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Tạo QR và giữ hàng" })).toHaveCount(0);
  await page.getByLabel("Phương thức").selectOption("Tiền mặt");
  await page.getByLabel("Khách đưa").fill("1000");
  await expect(page.getByRole("button", { name: /Xác nhận thanh toán/ })).toBeEnabled();
});
