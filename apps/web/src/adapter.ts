import { accounts, login as demoLogin, type User } from "./api";

export type AuthMode = "demo" | "api";

export interface AuthAdapter {
  readonly mode: AuthMode;
  login(id: string, password: string): Promise<User>;
  restoreSession(): Promise<User | undefined>;
}

export class DemoAuthAdapter implements AuthAdapter {
  readonly mode = "demo" as const;

  async login(id: string, password: string): Promise<User> {
    // Simulate network delay for demo
    await new Promise((resolve) => setTimeout(resolve, 300));
    return demoLogin(id, password);
  }

  async restoreSession(): Promise<User | undefined> {
    const id = sessionStorage.getItem("simtim-v2-user");
    return accounts.find((account) => account.id === id);
  }
}

export class ApiAuthAdapter implements AuthAdapter {
  readonly mode = "api" as const;

  async login(id: string, password: string): Promise<User> {
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ id, password }),
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Mã nhân viên hoặc mật khẩu chưa đúng.");
        }
        throw new Error(`Lỗi kết nối máy chủ (${res.status})`);
      }
      return parseUser(await res.json());
    } catch (e) {
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể kết nối đến máy chủ.", { cause: e });
      }
      throw e;
    }
  }

  async restoreSession(): Promise<User | undefined> {
    try {
      const res = await fetch("/api/me", { credentials: "include" });
      if (res.status === 401) return undefined;
      if (!res.ok) throw new Error(`Không thể khôi phục phiên đăng nhập (${res.status}).`);
      return parseUser(await res.json());
    } catch (e) {
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể khôi phục phiên đăng nhập.", { cause: e });
      }
      throw e;
    }
  }
}

function parseUser(value: unknown): User {
  if (!isUser(value)) {
    throw new Error("Phản hồi phiên đăng nhập từ máy chủ không hợp lệ.");
  }
  return value;
}

function isUser(value: unknown): value is User {
  if (!value || typeof value !== "object") return false;
  const user = value as Record<string, unknown>;
  return (
    typeof user.id === "string" &&
    user.id.trim().length > 0 &&
    typeof user.name === "string" &&
    user.name.trim().length > 0 &&
    (user.role === "sales" || user.role === "stock" || user.role === "manager")
  );
}

export const authAdapter: AuthAdapter =
  typeof (import.meta as any).env !== "undefined" &&
  (import.meta as any).env.VITE_USE_API === "true"
    ? new ApiAuthAdapter()
    : new DemoAuthAdapter();
