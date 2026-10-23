import { accounts, login as demoLogin, permissions, type User } from "./api";

export type AuthMode = "demo" | "api";
const serverRoles = ["SALES", "STOCK", "MANAGER", "ADMIN"] as const;
export interface ServerSession {
  userId: string;
  organizationId: string;
  storeId: string;
  fullName: string;
  trainingEnabled: boolean;
  roles: (typeof serverRoles)[number][];
  permissions: string[];
}
export type AuthUser = Omit<User, "role"> & {
  role: User["role"] | "admin";
  session?: ServerSession;
};

export function routesFor(user: AuthUser): string[] {
  if (!user.session) return user.role === "admin" ? [] : permissions[user.role];
  return [
    "dashboard",
    ...(user.session.permissions.includes("catalog.read") ? ["products", "suppliers"] : []),
    ...(user.session.permissions.includes("receipts.read") ||
    user.session.permissions.includes("receipts.write")
      ? ["receive"]
      : []),
    ...(user.session.permissions.includes("inventory.read") ? ["inventory", "count"] : []),
    ...(user.session.permissions.includes("sales.write") ||
    user.session.permissions.includes("sales.create")
      ? ["sale"]
      : []),
    ...(user.session.permissions.includes("sales.read.own") ||
    user.session.permissions.includes("sales.read.store") ||
    user.session.permissions.includes("sales.write") ||
    user.session.permissions.includes("sales.create")
      ? ["invoices"]
      : []),
    ...(user.session.permissions.includes("promotions.read") ? ["promotions"] : []),
    ...(user.session.permissions.includes("reports.read") ? ["reports"] : []),
    ...(user.session.trainingEnabled ? ["training"] : []),
  ];
}

export interface AuthAdapter {
  readonly mode: AuthMode;
  login(id: string, password: string): Promise<AuthUser>;
  restoreSession(): Promise<AuthUser | undefined>;
  logout(): Promise<void>;
  fetchApi(path: string, init?: RequestInit, options?: { silentAuth?: boolean }): Promise<Response>;
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

  async logout(): Promise<void> {
    return;
  }

  async fetchApi(
    _path: string,
    _init?: RequestInit,
    _options?: { silentAuth?: boolean },
  ): Promise<Response> {
    void _path;
    void _init;
    void _options;
    throw new Error("API not available in demo mode");
  }
}

export class ApiAuthAdapter implements AuthAdapter {
  readonly mode = "api" as const;
  private accessToken?: string;
  private session?: ServerSession;
  private revision = 0;
  private restoreDisabled = false;

  constructor(
    private readonly organizationCode = "SIMTIM",
    private readonly storeCode = "MAIN",
  ) {}

  async login(id: string, password: string): Promise<AuthUser> {
    const revision = ++this.revision;
    try {
      const res = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-SimTim-Browser": "1" },
        signal: AbortSignal.timeout(15000),
        // Hosting protection may use a same-origin cookie; the API still requires Bearer.
        credentials: "same-origin",
        cache: "no-store",
        body: JSON.stringify({
          organizationCode: this.organizationCode,
          storeCode: this.storeCode,
          username: id,
          password,
        }),
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Mã nhân viên hoặc mật khẩu chưa đúng.");
        }
        throw new Error(`Lỗi kết nối máy chủ (${res.status})`);
      }
      const { accessToken, user } = parseGrant(await readJson(res));
      if (revision !== this.revision) throw invalidSession();
      this.accessToken = accessToken;
      this.session = user.session;
      this.restoreDisabled = false;
      return user;
    } catch (e) {
      if (e instanceof DOMException && e.name === "TimeoutError") {
        throw new Error("Máy chủ phản hồi chậm. Vui lòng thử đăng nhập lại.", { cause: e });
      }
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể kết nối đến máy chủ.", { cause: e });
      }
      throw e;
    }
  }

  async restoreSession(): Promise<AuthUser | undefined> {
    const token = this.accessToken;
    if (!token && this.restoreDisabled) return undefined;
    const revision = this.revision;
    try {
      const res = await fetch(token ? "/api/v1/auth/session" : "/api/v1/auth/restore", {
        method: token ? "GET" : "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : { "X-SimTim-Browser": "1" },
        signal: AbortSignal.timeout(15000),
        credentials: "same-origin",
        cache: "no-store",
      });
      if (revision !== this.revision) return undefined;
      if (res.status === 401) {
        this.accessToken = undefined;
        this.session = undefined;
        this.restoreDisabled = true;
        return undefined;
      }
      if (!res.ok) throw new Error(`Không thể khôi phục phiên đăng nhập (${res.status}).`);
      const value = await readJson(res);
      if (revision !== this.revision) return undefined;
      if (!token) {
        const restored = parseGrant(value);
        this.accessToken = restored.accessToken;
        this.session = restored.user.session;
        return restored.user;
      }
      const user = parseSession(value);
      this.session = user.session;
      return user;
    } catch (e) {
      if (e instanceof DOMException && e.name === "TimeoutError") {
        throw new Error("Máy chủ phản hồi chậm. Tải lại trang để thử khôi phục phiên.", {
          cause: e,
        });
      }
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể khôi phục phiên đăng nhập.", { cause: e });
      }
      throw e;
    }
  }

  async logout(): Promise<void> {
    this.revision++;
    this.restoreDisabled = true;
    const token = this.accessToken;
    this.accessToken = undefined;
    this.session = undefined;
    if (!token) return;
    try {
      const res = await fetch("/api/v1/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        credentials: "same-origin",
        cache: "no-store",
        signal: AbortSignal.timeout(15000),
      });
      if (!res.ok && res.status !== 401) {
        throw new Error(`Không thể thu hồi phiên trên máy chủ (${res.status}).`);
      }
    } catch (e) {
      if (e instanceof DOMException && e.name === "TimeoutError") {
        throw new Error("Chưa xác nhận được thu hồi phiên trên máy chủ do phản hồi chậm.", {
          cause: e,
        });
      }
      if (e instanceof TypeError) {
        throw new Error("Lỗi mạng: Không thể thu hồi phiên trên máy chủ.", { cause: e });
      }
      throw e;
    }
  }

  async fetchApi(
    path: string,
    init?: RequestInit,
    options?: { silentAuth?: boolean },
  ): Promise<Response> {
    const headers = new Headers(init?.headers);
    const requestToken = this.accessToken;
    if (requestToken) {
      headers.set("Authorization", `Bearer ${requestToken}`);
    }
    if (this.session) {
      headers.set("X-Organization-Id", this.session.organizationId);
      headers.set("X-Store-Id", this.session.storeId);
    }
    const response = await fetch(path, { ...init, headers, credentials: "same-origin" });
    if (!options?.silentAuth && response.status === 401 && requestToken === this.accessToken) {
      this.revision++;
      this.restoreDisabled = true;
      this.accessToken = undefined;
      this.session = undefined;
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("simtim:auth-required"));
      }
    }
    return response;
  }
}

function parseGrant(value: unknown): { accessToken: string; user: AuthUser } {
  if (!value || typeof value !== "object") throw invalidSession();
  const grant = value as Record<string, unknown>;
  if (
    typeof grant.accessToken !== "string" ||
    !/^[A-Za-z0-9_-]{43}$/.test(grant.accessToken) ||
    grant.tokenType !== "Bearer" ||
    typeof grant.expiresAt !== "string" ||
    !(Date.parse(grant.expiresAt) > Date.now())
  )
    throw invalidSession();
  return { accessToken: grant.accessToken, user: parseSession(grant.session) };
}

function invalidSession() {
  return new Error("Phản hồi phiên đăng nhập từ máy chủ không hợp lệ.");
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    throw invalidSession();
  }
}

function parseSession(value: unknown): AuthUser {
  if (!value || typeof value !== "object") throw invalidSession();
  const session = value as Record<string, unknown>;
  const uuid = /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;
  if (
    ![session.userId, session.organizationId, session.storeId].every(
      (id) => typeof id === "string" && uuid.test(id),
    ) ||
    typeof session.fullName !== "string" ||
    !session.fullName.trim() ||
    typeof session.trainingEnabled !== "boolean" ||
    !Array.isArray(session.roles) ||
    session.roles.length === 0 ||
    !session.roles.every((role) => serverRoles.includes(role)) ||
    !Array.isArray(session.permissions) ||
    !session.permissions.every((permission) => typeof permission === "string" && permission.trim())
  )
    throw invalidSession();
  const valid = session as unknown as ServerSession;
  return {
    id: valid.userId,
    name: valid.fullName,
    role: valid.roles[0].toLowerCase() as AuthUser["role"],
    session: valid,
  };
}

const env = import.meta.env;
export const authAdapter: AuthAdapter =
  env?.VITE_USE_API === "true"
    ? new ApiAuthAdapter(env.VITE_ORGANIZATION_CODE, env.VITE_STORE_CODE)
    : new DemoAuthAdapter();
