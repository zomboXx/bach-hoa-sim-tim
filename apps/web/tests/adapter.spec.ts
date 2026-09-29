import { expect, test } from "@playwright/test";
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
});
