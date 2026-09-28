import { login as demoLogin, type User } from "./api";

export interface AuthAdapter {
  login(id: string, password: string): Promise<User>;
}

export class DemoAuthAdapter implements AuthAdapter {
  async login(id: string, password: string): Promise<User> {
    // Simulate network delay for demo
    await new Promise((resolve) => setTimeout(resolve, 300));
    return demoLogin(id, password);
  }
}

export class ApiAuthAdapter implements AuthAdapter {
  async login(id: string, password: string): Promise<User> {
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id, password }),
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Mã nhân viên hoặc mật khẩu chưa đúng.");
        }
        throw new Error(`Lỗi kết nối máy chủ (${res.status})`);
      }
      return await res.json();
    } catch (e) {
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể kết nối đến máy chủ.", { cause: e });
      }
      throw e;
    }
  }
}

export const authAdapter: AuthAdapter =
  typeof (import.meta as any).env !== "undefined" &&
  (import.meta as any).env.VITE_USE_API === "true"
    ? new ApiAuthAdapter()
    : new DemoAuthAdapter();
