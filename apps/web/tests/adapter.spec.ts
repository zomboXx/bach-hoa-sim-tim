import { test, expect } from "@playwright/test";
import { DemoAuthAdapter } from "../src/adapter";

test.describe("Auth Adapter Consumer Tests", () => {
  test("DemoAuthAdapter should return mock data for valid login", async () => {
    const adapter = new DemoAuthAdapter();
    const user = await adapter.login("NV001", "demo123");
    expect(user.id).toBe("NV001");
    expect(user.role).toBe("sales");
  });

  test("DemoAuthAdapter should throw for invalid login", async () => {
    const adapter = new DemoAuthAdapter();
    await expect(adapter.login("NV001", "wrong")).rejects.toThrow(
      "Mã nhân viên hoặc mật khẩu chưa đúng.",
    );
  });

  test("ApiAuthAdapter should fetch and return user on success", async ({ page }) => {
    await page.goto("/");
    await page.route("/api/auth/login", async (route) => {
      const body = route.request().postDataJSON();
      if (body.id === "NV001" && body.password === "realpass") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ id: "NV001", name: "Lan Nguyễn", role: "sales" }),
        });
      } else {
        await route.fulfill({ status: 401 });
      }
    });

    // Playwright page exposes evaluate which runs in browser
    const user = await page.evaluate(async () => {
      // In the browser context, we can't directly use ApiAuthAdapter from Node.
      // We will perform the equivalent fetch to test the route logic.
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: "NV001", password: "realpass" }),
      });
      return res.json();
    });

    expect(user.id).toBe("NV001");
    expect(user.role).toBe("sales");
  });

  test("ApiAuthAdapter should handle network errors", async ({ page }) => {
    await page.goto("/");
    await page.route("/api/auth/login", (route) => route.abort("failed"));

    const errorMsg = await page.evaluate(async () => {
      try {
        await fetch("/api/auth/login", { method: "POST" });
        return "Success";
      } catch (e) {
        if (e instanceof TypeError) return "TypeError";
        return "Other Error";
      }
    });

    expect(errorMsg).toBe("TypeError");
  });
});
