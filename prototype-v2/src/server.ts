import type { State, User, Count } from "./api";
export const apiMode = import.meta.env.VITE_DATA_MODE !== "demo";
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
let csrf: { token: string; headerName: string } | undefined;
export async function request<T = unknown>(
  path: string,
  method = "GET",
  body?: unknown,
  key?: string,
): Promise<T> {
  if (method !== "GET" && !csrf) csrf = await request("/csrf");
  const headers: Record<string, string> = {};
  if (csrf && method !== "GET") headers[csrf.headerName] = csrf.token;
  if (key) headers["Idempotency-Key"] = key;
  if (body !== undefined)
    headers["Content-Type"] =
      body instanceof URLSearchParams
        ? "application/x-www-form-urlencoded"
        : "application/json";
  let response: Response;
  try {
    response = await fetch("/api" + path, {
      method,
      credentials: "same-origin",
      headers,
      body:
        body === undefined
          ? undefined
          : body instanceof URLSearchParams
            ? body
            : JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new ApiError(
      "Không kết nối được máy chủ. Thao tác chưa được xác nhận; kiểm tra mạng và thử lại.",
      0,
    );
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    if (response.status === 401 || response.status === 403) csrf = undefined;
    throw new ApiError(
      data.message ||
        (response.status === 401
          ? "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
          : "Máy chủ không xử lý được yêu cầu."),
      response.status,
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}
export async function serverLogin(id: string, password: string) {
  csrf = undefined;
  await request(
    "/auth/login",
    "POST",
    new URLSearchParams({ username: id.trim().toUpperCase(), password }),
  );
  csrf = undefined;
  return request<User>("/me");
}
export async function serverLogout() {
  await request("/auth/logout", "POST");
  csrf = undefined;
}
export const emptyState = (): State => ({
  products: [],
  batches: [],
  suppliers: [],
  invoices: [],
  movements: [],
  counts: [],
  receipts: [],
  promotions: [],
  categories: [],
});
const database = new Promise<IDBDatabase>((resolve, reject) => {
  const open = indexedDB.open("simtim-server-offline", 1);
  open.onupgradeneeded = () => open.result.createObjectStore("counts");
  open.onsuccess = () => resolve(open.result);
  open.onerror = () => reject(open.error);
});
export async function queueFor(userId: string): Promise<Count[]> {
  const db = await database;
  return new Promise((resolve, reject) => {
    const r = db.transaction("counts").objectStore("counts").get(userId);
    r.onsuccess = () => resolve(r.result || []);
    r.onerror = () => reject(r.error);
  });
}
export async function saveQueue(userId: string, rows: Count[]) {
  const db = await database;
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction("counts", "readwrite");
    tx.objectStore("counts").put(JSON.parse(JSON.stringify(rows)), userId);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
export async function serverState(userId: string) {
  const state = await request<State>("/state");
  state.counts.unshift(...(await queueFor(userId)));
  return state;
}
