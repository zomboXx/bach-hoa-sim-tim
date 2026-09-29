import { expect, test, type Page, type Route } from "@playwright/test";
import { ApiAuthAdapter, type ServerSession } from "../src/adapter";

const token = "a".repeat(43);
const stockSession: ServerSession = {
  userId: "00000000-0000-4000-8000-000000000001",
  organizationId: "00000000-0000-4000-8000-000000000002",
  storeId: "00000000-0000-4000-8000-000000000003",
  fullName: "Server Stock HS",
  trainingEnabled: false,
  roles: ["STOCK"],
  permissions: ["catalog.read", "catalog.write"],
};
const grant = (session = stockSession, accessToken = token) => ({
  accessToken,
  tokenType: "Bearer",
  expiresAt: new Date(Date.now() + 3600000).toISOString(),
  session,
});

async function fillCredentials(page: Page) {
  await page.getByLabel("Mã nhân viên", { exact: true }).fill("stock");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("server-pass");
}
async function signIn(page: Page) {
  await fillCredentials(page);
  await page.getByRole("button", { name: "Vào không gian làm việc" }).click();
}
async function fulfillJson(route: Route, status: number, body?: unknown) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

test("API login sends BE-02 scope and keeps credentials out of persistent storage", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/login", async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      organizationCode: "SIMTIM",
      storeCode: "MAIN",
      username: "stock",
      password: "server-pass",
    });
    await fulfillJson(route, 200, grant());
  });
  await page.goto("/");
  await signIn(page);
  await expect(page.locator(".user-name")).toContainText(stockSession.fullName);
  const stored = await page.evaluate(() => ({
    local: { ...localStorage },
    session: { ...sessionStorage },
    cookies: document.cookie,
  }));
  expect(JSON.stringify(stored)).not.toContain(token);
  expect(JSON.stringify(stored)).not.toContain("server-pass");
  expect(stored.session).not.toHaveProperty("simtim-v2-user");
  await page.reload();
  await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeEnabled();
  await expect(page.locator("nav")).toHaveCount(0);
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveValue("");
});

test("legacy API markers cannot restore a demo role or resurrect a logged-out workspace", async ({
  page,
}) => {
  let restores = 0;
  await page.addInitScript(() => {
    sessionStorage.setItem("simtim-v2-user", "QL001");
    sessionStorage.setItem("simtim-v2-auth-mode", "api");
  });
  await page.route(/\/api\/(me|v1\/auth\/session)$/, async (route) => {
    restores++;
    await fulfillJson(route, 200, { ...stockSession, roles: ["MANAGER"] });
  });
  await page.route("**/api/v1/auth/login", (route) => fulfillJson(route, 200, grant()));
  await page.route("**/api/v1/auth/logout", (route) => route.fulfill({ status: 204 }));
  await page.goto("/");
  await expect(page.locator("nav")).toHaveCount(0);
  await signIn(page);
  await expect(page.locator("nav")).toBeVisible();
  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeEnabled();
  await page.reload();
  await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeEnabled();
  await expect(page.locator("nav")).toHaveCount(0);
  expect(restores).toBe(0);
});

test("pending login blocks duplicate submission until the server grants the session", async ({
  page,
}) => {
  let releaseLogin: (() => void) | undefined;
  let loginRequests = 0;
  await page.route("**/api/v1/auth/login", async (route) => {
    loginRequests++;
    await new Promise<void>((resolve) => {
      releaseLogin = resolve;
    });
    await fulfillJson(route, 200, grant());
  });
  await page.goto("/");
  await signIn(page);
  await expect(page.getByRole("button", { name: "Đang đăng nhập…" })).toBeDisabled();
  await expect(page.locator("nav")).toHaveCount(0);
  await page.locator(".login-form form").evaluate((form) => {
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
  });
  await expect.poll(() => Boolean(releaseLogin)).toBe(true);
  expect(loginRequests).toBe(1);
  releaseLogin?.();
  await expect(page.locator("nav")).toBeVisible();
});

for (const role of ["SALES", "STOCK", "MANAGER", "ADMIN"] as const) {
  test(`${role} navigation follows server permissions and training state`, async ({ page }) => {
    await page.route("**/api/v1/auth/login", (route) =>
      fulfillJson(
        route,
        200,
        grant({
          ...stockSession,
          roles: [role],
          permissions: [],
          trainingEnabled: false,
        }),
      ),
    );
    await page.goto("/");
    await signIn(page);
    await expect(page.locator("nav").getByRole("button")).toHaveCount(1);
    await expect(page.locator("nav")).toContainText("Tổng quan");
    await expect(
      page.getByRole("button", { name: /đào tạo|Tạo đơn|Nhận hàng|Tra cứu sản phẩm/i }),
    ).toHaveCount(0);
  });
}

test("catalog access is read-only and training uses its independent server flag", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/login", (route) =>
    fulfillJson(
      route,
      200,
      grant({
        ...stockSession,
        roles: ["ADMIN", "MANAGER"],
        trainingEnabled: true,
      }),
    ),
  );
  await page.goto("/");
  await signIn(page);
  await expect(page.locator("nav").getByRole("button")).toHaveCount(4);
  await page
    .locator("nav")
    .getByRole("button", { name: /Sản phẩm/ })
    .click();
  await expect(page.getByRole("button", { name: /Thêm sản phẩm/ })).toHaveCount(0);
  await page
    .locator("nav")
    .getByRole("button", { name: /Nhà cung cấp/ })
    .click();
  await expect(page.getByRole("button", { name: /Thêm nhà cung cấp/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Mở đào tạo cùng Mentor Mai" }).click();
  await expect(page.getByRole("button", { name: /Bắt đầu ca thực hành/ })).toBeVisible();
});

test("logout sends Bearer, clears the workspace immediately, and blocks login until revocation finishes", async ({
  page,
}) => {
  let releaseLogout: (() => void) | undefined;
  await page.route("**/api/v1/auth/login", (route) => fulfillJson(route, 200, grant()));
  await page.route("**/api/v1/auth/logout", async (route) => {
    expect(route.request().method()).toBe("POST");
    expect(route.request().headers().authorization).toBe(`Bearer ${token}`);
    await new Promise<void>((resolve) => {
      releaseLogout = resolve;
    });
    await route.fulfill({ status: 204 });
  });
  await page.goto("/");
  await signIn(page);
  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await expect(page.locator("nav")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Đang đăng xuất…" })).toBeDisabled();
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveValue("");
  await expect.poll(() => Boolean(releaseLogout)).toBe(true);
  releaseLogout?.();
  await expect(page.getByRole("button", { name: "Vào không gian làm việc" })).toBeEnabled();
});

test("failed revocation reports an error while keeping the user logged out", async ({ page }) => {
  await page.route("**/api/v1/auth/login", (route) => fulfillJson(route, 200, grant()));
  await page.route("**/api/v1/auth/logout", (route) => route.abort("failed"));
  await page.goto("/");
  await signIn(page);
  await page.getByRole("button", { name: /Đăng xuất/ }).click();
  await expect(page.getByRole("alert")).toContainText("Không thể thu hồi phiên");
  await expect(page.locator("nav")).toHaveCount(0);
});

for (const status of [401, 403, 429, 500]) {
  test(`API login reports HTTP ${status} without opening a session`, async ({ page }) => {
    await page.route("**/api/v1/auth/login", (route) => fulfillJson(route, status));
    await page.goto("/");
    await signIn(page);
    await expect(page.getByRole("alert")).toContainText(
      status < 429 ? "chưa đúng" : String(status),
    );
    await expect(page.locator("nav")).toHaveCount(0);
  });
}

for (const [index, invalid] of [
  { id: "QL001", name: "Legacy User", role: "manager" },
  { ...grant(), accessToken: "invalid" },
  { ...grant(), expiresAt: "2000-01-01T00:00:00Z" },
  { ...grant(), session: { ...stockSession, permissions: undefined } },
  { ...grant(), session: { ...stockSession, roles: ["UNKNOWN"] } },
].entries()) {
  test(`rejects malformed grant ${index + 1}`, async ({ page }) => {
    await page.route("**/api/v1/auth/login", (route) => fulfillJson(route, 200, invalid));
    await page.goto("/");
    await signIn(page);
    await expect(page.getByRole("alert")).toContainText("không hợp lệ");
    await expect(page.locator("nav")).toHaveCount(0);
  });
}

test("API login reports a network failure in the login form", async ({ page }) => {
  await page.route("**/api/v1/auth/login", (route) => route.abort("failed"));
  await page.goto("/");
  await signIn(page);
  await expect(page.getByRole("alert")).toContainText("Lỗi mạng");
  await expect(page.locator("nav")).toHaveCount(0);
});

async function withFetch(stub: typeof fetch, check: () => Promise<void>) {
  const original = globalThis.fetch;
  globalThis.fetch = stub;
  try {
    await check();
  } finally {
    globalThis.fetch = original;
  }
}
const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
  });

test("session reads use Bearer and expired sessions discard the in-memory token", async () => {
  let sessionRequests = 0;
  await withFetch(
    async (input, init) => {
      if (String(input).endsWith("/login")) return jsonResponse(grant());
      expect(String(input)).toBe("/api/v1/auth/session");
      expect(new Headers(init?.headers).get("Authorization")).toBe(`Bearer ${token}`);
      sessionRequests++;
      return sessionRequests === 1
        ? jsonResponse(stockSession)
        : new Response(null, { status: 401 });
    },
    async () => {
      const adapter = new ApiAuthAdapter();
      await adapter.login("stock", "server-pass");
      expect((await adapter.restoreSession())?.name).toBe(stockSession.fullName);
      expect(await adapter.restoreSession()).toBeUndefined();
      expect(await adapter.restoreSession()).toBeUndefined();
      expect(sessionRequests).toBe(2);
    },
  );
});

for (const action of ["new login", "logout"] as const) {
  test(`late session response cannot override ${action}`, async () => {
    let release: ((response: Response) => void) | undefined;
    let logins = 0;
    await withFetch(
      async (input) => {
        if (String(input).endsWith("/login")) {
          logins++;
          return jsonResponse(
            grant({ ...stockSession, fullName: `User ${logins}` }, String(logins).repeat(43)),
          );
        }
        if (String(input).endsWith("/logout")) return new Response(null, { status: 204 });
        return new Promise<Response>((resolve) => {
          release = resolve;
        });
      },
      async () => {
        const adapter = new ApiAuthAdapter();
        await adapter.login("stock", "server-pass");
        const pending = adapter.restoreSession();
        if (action === "new login") await adapter.login("sales", "server-pass");
        else await adapter.logout();
        release?.(jsonResponse({ ...stockSession, roles: ["MANAGER"] }));
        expect(await pending).toBeUndefined();
      },
    );
  });
}
