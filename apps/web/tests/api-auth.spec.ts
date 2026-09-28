import { expect, test, type Page, type Route } from "@playwright/test";

const stockUser = { id: "QL001", name: "Server Stock HS", role: "stock" };

async function fillCredentials(page: Page, password = "server-pass") {
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("QL001");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
}

async function fulfillJson(route: Route, status: number, body?: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

test("API login uses the server user again after reload instead of a demo account", async ({
  page,
}) => {
  let loginRequests = 0;
  let sessionRequests = 0;
  await page.route("**/api/auth/login", async (route) => {
    loginRequests += 1;
    expect(route.request().postDataJSON()).toEqual({ id: "QL001", password: "server-pass" });
    await fulfillJson(route, 200, stockUser);
  });
  await page.route("**/api/me", async (route) => {
    sessionRequests += 1;
    await fulfillJson(route, 200, stockUser);
  });

  await page.goto("/");
  await fillCredentials(page);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(
    page.locator("nav").getByRole("button", { name: "Nhận hàng", exact: false }),
  ).toBeVisible();
  await expect(page.locator("nav").getByRole("button", { name: /Khuyến mãi|Báo cáo/ })).toHaveCount(
    0,
  );

  await page.reload();
  await expect(
    page.locator("nav").getByRole("button", { name: "Nhận hàng", exact: false }),
  ).toBeVisible();
  await expect(page.locator("nav").getByRole("button", { name: /Khuyến mãi|Báo cáo/ })).toHaveCount(
    0,
  );
  expect(loginRequests).toBe(1);
  expect(sessionRequests).toBe(1);
});

test("API login shows loading, blocks duplicate submission, and changes state only on success", async ({
  page,
}) => {
  let releaseLogin: (() => void) | undefined;
  let loginRequests = 0;
  await page.route("**/api/auth/login", async (route) => {
    loginRequests += 1;
    await new Promise<void>((resolve) => {
      releaseLogin = resolve;
    });
    await fulfillJson(route, 200, stockUser);
  });

  await page.goto("/");
  await fillCredentials(page);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  const submit = page.getByRole("button", { name: "Đang đăng nhập…" });
  await expect(submit).toBeDisabled();
  await expect(page.locator("nav")).toHaveCount(0);
  await page.locator("form").evaluate((form) => {
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
  });
  expect(loginRequests).toBe(1);
  await expect.poll(() => Boolean(releaseLogin)).toBe(true);
  releaseLogin?.();
  await expect(page.locator("nav")).toBeVisible();
});

for (const status of [401, 403]) {
  test(`API login reports HTTP ${status} without opening a session`, async ({ page }) => {
    await page.route("**/api/auth/login", (route) => fulfillJson(route, status));
    await page.goto("/");
    await fillCredentials(page);
    await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
    await expect(page.getByRole("alert")).toContainText("chưa đúng");
    await expect(page.locator("nav")).toHaveCount(0);
  });
}

test("API login rejects a server response with a role unsupported by this PWA", async ({
  page,
}) => {
  await page.route("**/api/auth/login", (route) =>
    fulfillJson(route, 200, { id: "ADMIN-001", name: "Nhân viên kho", role: "admin" }),
  );
  await page.goto("/");
  await fillCredentials(page);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.getByRole("alert")).toContainText("không hợp lệ");
  await expect(page.locator("nav")).toHaveCount(0);
});

test("API login reports a network failure in the login form", async ({ page }) => {
  await page.route("**/api/auth/login", (route) => route.abort("failed"));
  await page.goto("/");
  await fillCredentials(page);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
  await expect(page.getByRole("alert")).toContainText("Lỗi mạng");
  await expect(page.locator("nav")).toHaveCount(0);
});
