// Demo adapter: replace with REST requests when the server contract is implemented.
// All data here is device-local. This is not authentication or server authorization.
export type Role = "admin" | "accountant" | "manager" | "sales" | "stock";
export const roleNames: Record<Role, string> = {
  admin: "Admin / Chủ cửa hàng",
  accountant: "Kế toán",
  manager: "Quản lý cửa hàng",
  sales: "Nhân viên bán hàng / Thu ngân",
  stock: "Nhân viên kho",
};
export const startPage: Record<Role, string> = {
  admin: "dashboard",
  accountant: "finance",
  manager: "approvals",
  sales: "sale",
  stock: "receive",
};
export interface User {
  id: string;
  name: string;
  role: Role;
}
export interface Product {
  active?: boolean;
  id: string;
  name: string;
  barcode: string;
  category: string;
  unit: string;
  price: number;
  emoji: string;
}
export interface Batch {
  version?: number;
  id: string;
  productId: string;
  quantity: number;
  expiry: string;
}
export interface Supplier {
  active?: boolean;
  id: string;
  name: string;
  phone: string;
}
export interface Invoice {
  id: string;
  at: string;
  staff: string;
  method: string;
  total: number;
  lines: { name: string; quantity: number; price: number }[];
}
export interface Movement {
  id: string;
  at: string;
  productId: string;
  quantity: number;
  kind: string;
  reference: string;
}
export interface Count {
  baseVersion?: number;
  id: string;
  at: string;
  productId: string;
  batchId: string;
  expected: number;
  actual: number;
  note: string;
  status: "PENDING" | "REVIEW" | "APPROVED" | "CONFLICT";
}
export interface State {
  categories?: { id: string; name: string; active: boolean }[];
  products: Product[];
  batches: Batch[];
  suppliers: Supplier[];
  invoices: Invoice[];
  movements: Movement[];
  counts: Count[];
  promotions: { productId: string; percent: number; end: string }[];
  receipts: {
    id: string;
    supplier: string;
    product: string;
    accepted: number;
    rejected: number;
    note: string;
  }[];
}
export const localDay = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const day = (offset: number) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return localDay(d);
};
export const today = () => day(0);
const seed = (): State => ({
  products: [
    {
      id: "SP001",
      name: "Sữa tươi ít đường",
      barcode: "8934673120010",
      category: "Sữa & đồ uống",
      unit: "Hộp",
      price: 8500,
      emoji: "🥛",
    },
    {
      id: "SP002",
      name: "Bánh quy bơ",
      barcode: "8934673120027",
      category: "Bánh kẹo",
      unit: "Gói",
      price: 32000,
      emoji: "🍪",
    },
    {
      id: "SP003",
      name: "Trà đào thanh mát",
      barcode: "8934673120034",
      category: "Sữa & đồ uống",
      unit: "Chai",
      price: 16000,
      emoji: "🍑",
    },
    {
      id: "SP004",
      name: "Mì rau củ",
      barcode: "8934673120041",
      category: "Thực phẩm khô",
      unit: "Gói",
      price: 6500,
      emoji: "🍜",
    },
    {
      id: "SP005",
      name: "Nước giặt dịu nhẹ",
      barcode: "8934673120058",
      category: "Chăm sóc nhà cửa",
      unit: "Túi",
      price: 89000,
      emoji: "🫧",
    },
    {
      id: "SP006",
      name: "Trứng gà tươi",
      barcode: "8934673120065",
      category: "Thực phẩm tươi",
      unit: "Hộp",
      price: 28000,
      emoji: "🥚",
    },
  ],
  batches: [
    ["SP001", 48, 3],
    ["SP002", 32, 90],
    ["SP003", 24, 30],
    ["SP004", 80, 120],
    ["SP005", 7, 200],
    ["SP006", 12, 6],
    ["SP001", 6, -2],
  ].map((x, i) => ({
    id: `LO0${i + 1}`,
    productId: String(x[0]),
    quantity: Number(x[1]),
    expiry: day(Number(x[2])),
  })),
  suppliers: [
    { id: "NCC01", name: "Phân phối An Nhiên", phone: "0901234567" },
    { id: "NCC02", name: "Thực phẩm Vườn Nhà", phone: "0912345678" },
  ],
  invoices: [],
  movements: [],
  counts: [],
  promotions: [{ productId: "SP001", percent: 10, end: day(3) }],
  receipts: [],
});
const dbPromise = new Promise<IDBDatabase>((resolve, reject) => {
  const req = indexedDB.open("simtim-v2", 1);
  req.onupgradeneeded = () => req.result.createObjectStore("state");
  req.onsuccess = () => resolve(req.result);
  req.onerror = () => reject(req.error);
});
export async function readState(): Promise<State> {
  const db = await dbPromise;
  return new Promise((resolve, reject) => {
    const request = db.transaction("state").objectStore("state").get("demo");
    request.onsuccess = () => resolve(request.result || seed());
    request.onerror = () => reject(request.error);
  });
}
export async function persist(state: State) {
  const db = await dbPromise;
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction("state", "readwrite");
    tx.objectStore("state").put(JSON.parse(JSON.stringify(state)), "demo");
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
export const uid = (prefix: string) => `${prefix}-${crypto.randomUUID().slice(0, 8).toUpperCase()}`;
export const accounts: User[] = [
  { id: "NV001", name: "Lan Nguyễn", role: "sales" },
  { id: "KHO001", name: "Minh Trần", role: "stock" },
  { id: "QL001", name: "An Phạm", role: "manager" },
  { id: "ADMIN001", name: "Chủ cửa hàng", role: "admin" },
  { id: "KT001", name: "Thu Hà", role: "accountant" },
];
export function login(id: string, password: string): User {
  const user = accounts.find((a) => a.id === id.trim().toUpperCase());
  if (!user || password !== "demo123") throw Error("Mã nhân viên hoặc mật khẩu chưa đúng.");
  return user;
}
export const permissions: Record<Role, string[]> = {
  sales: ["sale", "invoices", "training"],
  accountant: ["finance", "invoices", "training"],
  stock: ["receive", "inventory", "warehouse_orders", "training"],
  admin: [
    "dashboard",
    "sale",
    "invoices",
    "products",
    "suppliers",
    "receive",
    "inventory",
    "count",
    "promotions",
    "reports",
    "finance",
    "approvals",
    "warehouse_orders",
    "system",
    "training",
  ],
  manager: [
    "dashboard",
    "approvals",
    "invoices",
    "products",
    "suppliers",
    "inventory",
    "count",
    "promotions",
    "reports",
    "training",
  ],
};
export function available(s: State, productId: string) {
  return s.batches
    .filter((b) => b.productId === productId && b.expiry >= today())
    .reduce((sum, b) => sum + b.quantity, 0);
}
export function price(s: State, p: Product) {
  const promo = s.promotions.find((x) => x.productId === p.id && x.end >= today());
  return Math.round(p.price * (1 - (promo?.percent || 0) / 100));
}
export function checkout(
  s: State,
  user: User,
  lines: { id: string; quantity: number }[],
  method: string,
): Invoice {
  if (!permissions[user.role].includes("sale")) throw Error("Tài khoản không có quyền bán hàng.");
  if (!lines.length) throw Error("Thêm sản phẩm vào giỏ trước khi thanh toán.");
  for (const l of lines)
    if (!Number.isInteger(l.quantity) || l.quantity < 1 || available(s, l.id) < l.quantity)
      throw Error("Số lượng vượt tồn khả dụng.");
  const invoice: Invoice = {
    id: uid("HD"),
    at: new Date().toISOString(),
    staff: user.name,
    method,
    total: 0,
    lines: [],
  };
  for (const l of lines) {
    const p = s.products.find((p) => p.id === l.id)!;
    invoice.lines.push({
      name: p.name,
      quantity: l.quantity,
      price: price(s, p),
    });
    invoice.total += price(s, p) * l.quantity;
    let remaining = l.quantity;
    for (const b of s.batches
      .filter((b) => b.productId === l.id && b.expiry >= today())
      .sort((a, b) => a.expiry.localeCompare(b.expiry))) {
      const take = Math.min(b.quantity, remaining);
      b.quantity -= take;
      remaining -= take;
    }
    s.movements.unshift({
      id: uid("BD"),
      at: invoice.at,
      productId: l.id,
      quantity: -l.quantity,
      kind: "Bán hàng",
      reference: invoice.id,
    });
  }
  s.invoices.unshift(invoice);
  return invoice;
}
