<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import TrainingGame from "./training/TrainingGame.vue";
import {
  TrainingApiAdapter,
  TrainingRun,
  type TrainingActionInput,
  type TrainingHistoryItem,
  type TrainingSavedResult,
} from "./training/api-training-adapter";
import {
  accounts,
  available,
  emptyOperationalState,
  persist,
  price,
  readState,
  today,
  localDay,
  uid,
  type State,
  type User,
} from "./api";
import { authAdapter, routesFor, type AuthUser } from "./adapter";
import CatalogWorkspace from "./modules/catalog/CatalogWorkspace.vue";
import { ApiCatalogAdapter } from "./modules/catalog/api-catalog-adapter";
import InventoryWorkspace from "./modules/inventory/presentation/InventoryWorkspace.vue";
import StocktakeWorkspace from "./modules/inventory/presentation/StocktakeWorkspace.vue";
import { ApiInventoryAdapter } from "./modules/inventory/infrastructure/api-inventory-adapter";
import {
  ApiStocktakeAdapter,
  StocktakeApiError,
  type ApiStocktakeLine,
} from "./modules/inventory/infrastructure/api-stocktake-adapter";
import { DemoInventoryAdapter } from "./modules/inventory/infrastructure/demo-inventory-adapter";
import type { InventoryPort } from "./modules/inventory/domain/inventory";
import {
  evaluateStocktakeRetry,
  validateStocktakeQuantity,
  type StocktakeQueueItem,
  type StocktakeRecord,
  type StocktakeRetryContext,
  type StocktakeSubmission,
} from "./modules/inventory/domain/stocktake";
import { IndexedDbStocktakeOperationQueue } from "./modules/inventory/infrastructure/stocktake-operation-queue";
import PromotionWorkspace from "./modules/promotion/PromotionWorkspace.vue";
import { ApiPromotionAdapter } from "./modules/promotion/api-promotion-adapter";
import { ApiSaleAdapter } from "./modules/sale/infrastructure/api-sale-adapter";
import { useSharedPosCart } from "./modules/sale/infrastructure/shared-pos-cart";
import BarcodeScanner from "./modules/sale/presentation/BarcodeScanner.vue";
import { DemoSaleAdapter } from "./modules/sale/infrastructure/demo-sale-adapter";
import {
  SaleApiError,
  SaleNetworkError,
  type ApiInvoice,
  type CartLine,
  type CheckoutRequest,
  type QuoteResult,
  type SalePort,
} from "./modules/sale/domain/sale";
const state = ref<State>();
const user = ref<AuthUser>();
const route = ref("dashboard");
const isTrainingRoute = ref(
  typeof window !== "undefined" &&
    (window.location.pathname === "/training" ||
      window.location.pathname.startsWith("/training/") ||
      window.location.hash === "#training" ||
      window.location.hash.startsWith("#/training") ||
      new URLSearchParams(window.location.search).has("training")),
);

function exitTrainingRoute() {
  if (typeof window !== "undefined") {
    if (window.location.pathname === "/training") {
      window.history.pushState({}, "", "/");
      isTrainingRoute.value = false;
    } else {
      isTrainingRoute.value = false;
    }
  }
}

if (typeof window !== "undefined") {
  window.addEventListener("popstate", () => {
    isTrainingRoute.value =
      window.location.pathname === "/training" ||
      window.location.pathname.startsWith("/training/") ||
      window.location.hash === "#training" ||
      window.location.hash.startsWith("#/training") ||
      new URLSearchParams(window.location.search).has("training");
  });
}
const error = ref("");
const toast = ref("");
let timer: ReturnType<typeof setTimeout>;
const busy = ref(false);
const authPending = ref<"restore" | "login" | "logout" | undefined>("restore");
let authRevision = 0;
const menu = ref(false);
const online = ref(navigator.onLine);
const simulateOffline = ref(false);
const connected = computed(() => online.value && !simulateOffline.value);
const isDemoMode = authAdapter.mode === "demo";
const trainingApi = isDemoMode
  ? undefined
  : new TrainingApiAdapter((path, init) => authAdapter.fetchApi(path, init));
let trainingRun: TrainingRun | undefined;
const trainingStatus = ref("");
const trainingSaveError = ref(false);
const trainingSavedResult = ref<TrainingSavedResult>();
const trainingHistory = ref<TrainingHistoryItem[]>([]);

async function loadTrainingHistory() {
  if (!trainingApi || !user.value) return;
  try {
    trainingHistory.value = await trainingApi.ownResults();
  } catch {
    // A history read must not prevent starting a new practice session.
  }
}

function onTrainingStart() {
  trainingSavedResult.value = undefined;
  trainingSaveError.value = false;
  if (!trainingApi) {
    trainingStatus.value = "Bản chơi thử trên trình duyệt; kết quả chưa lưu trên máy chủ.";
    return;
  }
  if (!user.value) {
    trainingStatus.value = "Đăng nhập trong ứng dụng để lưu kết quả đào tạo.";
    return;
  }
  const run = new TrainingRun(trainingApi, (message, result, failed) => {
    if (trainingRun !== run) return;
    trainingStatus.value = message;
    trainingSaveError.value = !!failed;
    if (result) {
      trainingSavedResult.value = result;
      void loadTrainingHistory();
    }
  });
  trainingRun = run;
  void run.start();
}

function onTrainingAction(action: TrainingActionInput) {
  trainingRun?.append(action);
}

function onTrainingReset() {
  const previousRun = trainingRun;
  trainingRun = undefined;
  if (previousRun) void previousRun.abandon().catch(() => undefined);
  trainingSavedResult.value = undefined;
  trainingSaveError.value = false;
  trainingStatus.value = "";
}

function onTrainingExit() {
  onTrainingReset();
  if (isTrainingRoute.value) exitTrainingRoute();
  else go("dashboard");
}
const inventoryAdapter: InventoryPort = isDemoMode
  ? new DemoInventoryAdapter(
      () => state.value!,
      async (next) => {
        await persist(next);
        state.value = next;
      },
    )
  : new ApiInventoryAdapter((path, init) => authAdapter.fetchApi(path, init));
const promotionAdapter = new ApiPromotionAdapter((path, init) => authAdapter.fetchApi(path, init));
const catalogAdapter = new ApiCatalogAdapter((path, init) => authAdapter.fetchApi(path, init));
const saleAdapter: SalePort = isDemoMode
  ? new DemoSaleAdapter(
      () => state.value!,
      async (next) => {
        await persist(next);
        state.value = next;
      },
      () => ({ ...user.value!, role: user.value!.role }) as User,
    )
  : new ApiSaleAdapter((path, init) => authAdapter.fetchApi(path, init));
// API-mode invoice list (populated on navigate to 'invoices' and after checkout)
const apiInvoices = ref<ApiInvoice[]>([]);
const invoicesStatus = ref<"idle" | "loading" | "error">("idle");
async function loadInvoices() {
  if (isDemoMode) return;
  invoicesStatus.value = "loading";
  try {
    apiInvoices.value = await saleAdapter.listInvoices();
    invoicesStatus.value = "idle";
  } catch {
    invoicesStatus.value = "error";
  }
}
// API-mode product catalog for the sale page (loaded from /api/v1/products)
const apiSaleProducts = ref<
  Array<{ id: string; name: string; sku: string; category: string; unit: string; emoji: string }>
>([]);
async function loadSaleProducts() {
  if (isDemoMode) return;
  try {
    const [res, unitsRes] = await Promise.all([
      authAdapter.fetchApi("/api/v1/products"),
      authAdapter.fetchApi("/api/v1/units"),
    ]);
    if (!res.ok) return;
    const raw = (await res.json()) as Array<{
      id: string;
      name: string;
      sku?: string;
      status?: string;
      baseUnitId?: string;
    }>;
    const units = unitsRes.ok
      ? ((await unitsRes.json()) as Array<{ id: string; code: string }>)
      : [];
    const unitCode = new Map(units.map((unit) => [unit.id, unit.code]));
    apiSaleProducts.value = raw
      .filter((p) => !p.status || p.status === "ACTIVE")
      .map((p) => ({
        id: p.id,
        name: p.name,
        sku: p.sku ?? p.id,
        category: "Sản phẩm",
        unit: unitCode.get(p.baseUnitId ?? "") ?? "—",
        emoji: "὎6",
      }));
  } catch {
    // silently ignore — sale page will show empty grid
  }
}
const apiStocktakeAdapter = isDemoMode
  ? undefined
  : new ApiStocktakeAdapter((path, init) => authAdapter.fetchApi(path, init));
const stocktakeQueue = new IndexedDbStocktakeOperationQueue();
const queuedStocktakes = ref<StocktakeQueueItem[]>([]);
const apiStocktakeRecords = ref<StocktakeRecord[]>([]);
let stocktakeSyncPromise: Promise<void> | undefined;
const canManagePromotions = computed(
  () => !!user.value?.session?.permissions.includes("promotions.write"),
);
const canConfirmReceipt = computed(
  () =>
    isDemoMode ||
    (!!user.value?.session && user.value.session.permissions.includes("receipts.write")),
);
const credentials = reactive({
  id: isDemoMode ? "NV001" : "",
  password: isDemoMode ? "demo123" : "",
});
const roleNames = {
  sales: "Nhân viên bán hàng",
  stock: "Nhân viên hàng hóa",
  manager: "Quản lý cửa hàng",
  admin: "Quản trị viên",
};
const nav = [
  ["dashboard", "Tổng quan", "◫"],
  ["sale", "Bán hàng", "＋"],
  ["invoices", "Hóa đơn", "▤"],
  ["products", "Sản phẩm", "▦"],
  ["suppliers", "Nhà cung cấp", "♧"],
  ["receive", "Nhận hàng", "↓"],
  ["inventory", "Tồn kho & lô hàng", "▥"],
  ["count", "Kiểm kê", "☑"],
  ["promotions", "Khuyến mãi", "%"],
  ["reports", "Báo cáo", "↗"],
  ["training", "Đào tạo nghiệp vụ", "✦"],
];
const canGo = (id: string) => !!user.value && routesFor(user.value).includes(id);
const allowedNav = computed(() => nav.filter(([id]) => canGo(id)));
const stocktakeStoreId = computed(() => user.value?.session?.storeId || "MAIN");
const canWriteStocktake = computed(
  () =>
    (isDemoMode && canGo("count")) ||
    !!user.value?.session?.permissions.includes("stocktakes.write"),
);
function stocktakeRetryContext(): StocktakeRetryContext | undefined {
  if (!user.value) return undefined;
  return {
    actorId: user.value.session?.userId || user.value.id,
    organizationId: user.value.session?.organizationId || "DEMO",
    storeId: stocktakeStoreId.value,
    canSync: canWriteStocktake.value,
  };
}
const stocktakeRecords = computed<StocktakeRecord[]>(() => {
  const context = stocktakeRetryContext();
  const savedRecords =
    state.value?.counts.map((record) => ({
      id: record.id,
      clientOperationId: record.clientOperationId,
      countedAt: record.at,
      productId: record.productId,
      batchId: record.batchId,
      expectedQuantity: record.expected,
      actualQuantity: record.actual,
      note: record.note,
      status: record.status,
    })) || [];
  const savedOperationIds = new Set(
    savedRecords.flatMap((record) => (record.clientOperationId ? [record.clientOperationId] : [])),
  );
  const apiOperationIds = new Set(
    apiStocktakeRecords.value.flatMap((record) =>
      record.clientOperationId ? [record.clientOperationId] : [],
    ),
  );
  const queuedRecords = queuedStocktakes.value
    .filter(
      (operation) =>
        !savedOperationIds.has(operation.clientOperationId) &&
        !apiOperationIds.has(operation.clientOperationId),
    )
    .map((operation) => {
      const decision = context
        ? evaluateStocktakeRetry(operation, context)
        : { allowed: false, reason: "Cần đăng nhập để đồng bộ thao tác này." };
      return {
        id: operation.clientOperationId,
        clientOperationId: operation.clientOperationId,
        countedAt: operation.countedAt,
        productId: operation.productId,
        batchId: operation.batchId,
        expectedQuantity: operation.expectedQuantity,
        actualQuantity: operation.actualQuantity,
        note: operation.note,
        status: operation.status,
        syncBlockedReason: decision.allowed ? undefined : decision.reason,
      } satisfies StocktakeRecord;
    });
  return [...queuedRecords, ...apiStocktakeRecords.value, ...savedRecords];
});
const stocktakeWriteUnavailableReason = computed(() =>
  isDemoMode
    ? "Bạn không có quyền ghi nhận kiểm kê tại cửa hàng này."
    : "Phiên hiện tại không có quyền gửi kiểm kê cho cửa hàng này.",
);
const pageTitle = computed(() => nav.find(([id]) => id === route.value)?.[1]);
const money = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);
const time = (s: string) =>
  new Date(s).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
  });
const name = (id: string) =>
  (isDemoMode
    ? state.value?.products.find((p) => p.id === id)?.name
    : apiSaleProducts.value.find((p) => p.id === id)?.name) || id;
const dateText = new Date().toLocaleDateString("vi-VN", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});
const sales = computed(
  () => state.value?.invoices.filter((i) => localDay(new Date(i.at)) === today()) || [],
);
const revenue = computed(() => sales.value.reduce((n, i) => n + i.total, 0));
const alerts = computed(
  () =>
    state.value?.batches.filter(
      (b) =>
        b.quantity > 0 && Math.ceil((Date.parse(b.expiry) - Date.parse(today())) / 86400000) <= 7,
    ) || [],
);
const pending = computed(() => {
  const context = stocktakeRetryContext();
  if (!context) return 0;
  return queuedStocktakes.value.filter(
    (operation) =>
      operation.status === "PENDING" &&
      operation.actorId === context.actorId &&
      operation.organizationId === context.organizationId &&
      operation.storeId === context.storeId,
  ).length;
});
function notify(message: string) {
  toast.value = message;
  clearTimeout(timer);
  timer = setTimeout(() => (toast.value = ""), 4000);
}
function businessDay() {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
}
const reportPeriod = reactive({ from: businessDay(), to: businessDay() });
const reportStatus = ref<"loading" | "error" | "ready" | "empty">("ready");
const apiReport = ref<{ revenue: number; invoiceCount: number; inventory: any[] }>();
async function loadReports() {
  if (isDemoMode) {
    reportStatus.value = "ready";
    return;
  }
  try {
    reportStatus.value = "loading";
    const toDate = new Date(`${reportPeriod.to}T00:00:00Z`);
    toDate.setUTCDate(toDate.getUTCDate() + 1);
    const toStr = toDate.toISOString().slice(0, 10);
    const [revRes, invRes] = await Promise.all([
      authAdapter.fetchApi(`/api/v1/reports/revenue?from=${reportPeriod.from}&to=${toStr}`),
      authAdapter.fetchApi(`/api/v1/reports/inventory`),
    ]);
    if (!revRes.ok || !invRes.ok) throw new Error("Lỗi tải báo cáo");
    const revData = await revRes.json();
    const invData = await invRes.json();
    apiReport.value = {
      revenue: revData.revenue,
      invoiceCount: revData.invoiceCount,
      inventory: invData.items,
    };
    if (revData.invoiceCount === 0 && invData.items.length === 0) {
      reportStatus.value = "empty";
    } else {
      reportStatus.value = "ready";
    }
  } catch {
    reportStatus.value = "error";
  }
}
function go(id: string) {
  if (!canGo(id)) return;
  route.value = id;
  query.value = "";
  category.value = "Tất cả";
  menu.value = false;
  error.value = "";
  window.scrollTo(0, 0);
  if (id === "reports") {
    reportPeriod.from = businessDay();
    reportPeriod.to = businessDay();
    loadReports();
  }
  if (id === "invoices") {
    void loadInvoices();
  }
  if (id === "sale") {
    restorePendingCheckout();
    void loadSaleProducts();
  }
  if (id === "training") void loadTrainingHistory();
}
async function signIn() {
  if (busy.value || authPending.value) return;
  const revision = ++authRevision;
  authPending.value = "login";
  try {
    if (!connected.value) throw Error("Cần kết nối để bắt đầu phiên đăng nhập.");
    busy.value = true;
    const signedIn = await authAdapter.login(credentials.id, credentials.password);
    if (revision !== authRevision) return;
    user.value = signedIn;
    if (isDemoMode) {
      sessionStorage.setItem("simtim-v2-user", signedIn.id);
      sessionStorage.setItem("simtim-v2-auth-mode", "demo");
    } else clearSessionMarkers();
    credentials.password = "";
    go("dashboard");
    error.value = "";
  } catch (e) {
    if (revision === authRevision) error.value = (e as Error).message;
  } finally {
    if (revision === authRevision) {
      busy.value = false;
      authPending.value = undefined;
    }
  }
  if (revision === authRevision && user.value) {
    try {
      await refreshStocktakeQueue();
      if (connected.value) await sync();
    } catch (e) {
      error.value = (e as Error).message;
    }
  }
}
function clearSessionMarkers() {
  sessionStorage.removeItem("simtim-v2-user");
  sessionStorage.removeItem("simtim-v2-auth-mode");
}
async function signOut() {
  onTrainingReset();
  const revision = ++authRevision;
  authPending.value = "logout";
  user.value = undefined;
  clearSessionMarkers();
  cart.value = [];
  credentials.password = "";
  route.value = "dashboard";
  error.value = "";
  try {
    await authAdapter.logout();
  } catch (e) {
    if (revision === authRevision) error.value = (e as Error).message;
  } finally {
    if (revision === authRevision) authPending.value = undefined;
  }
}
async function change(action: (s: State) => void, message: string, allowOffline = false) {
  if (!isDemoMode) {
    notify("Chức năng này chưa kết nối dữ liệu cửa hàng.");
    return;
  }
  if (busy.value || !state.value) return;
  error.value = "";
  if (!connected.value && !allowOffline) {
    notify("Cần kết nối để thực hiện thao tác này. Chỉ kiểm kê hỗ trợ offline.");
    return;
  }
  busy.value = true;
  try {
    const next = JSON.parse(JSON.stringify(state.value)) as State;
    action(next);
    await persist(next);
    state.value = next;
    notify(message);
    return true;
  } catch (e) {
    notify((e as Error).message);
    return false;
  } finally {
    busy.value = false;
  }
}
async function refreshStocktakeQueue() {
  queuedStocktakes.value = await stocktakeQueue.list();
}
async function sync() {
  if (isTrainingRoute.value || route.value === "training") return;
  if (stocktakeSyncPromise) await stocktakeSyncPromise;

  const context = stocktakeRetryContext();
  if (!connected.value || !context || !state.value) return;
  const operations = queuedStocktakes.value.filter(
    (operation) => evaluateStocktakeRetry(operation, context).allowed,
  );
  if (!operations.length) return;

  const currentSync = syncStocktakeOperations(operations);
  stocktakeSyncPromise = currentSync;
  try {
    await currentSync;
  } finally {
    if (stocktakeSyncPromise === currentSync) stocktakeSyncPromise = undefined;
  }
}

async function syncStocktakeOperations(operations: StocktakeQueueItem[]) {
  error.value = "";
  try {
    if (!isDemoMode) {
      await syncApiStocktakes(operations);
      return;
    }
    const next = JSON.parse(JSON.stringify(state.value)) as State;
    const completed: string[] = [];
    const conflicts: { clientOperationId: string; reason: string }[] = [];
    let changed = false;

    for (const operation of operations) {
      if (next.counts.some((count) => count.clientOperationId === operation.clientOperationId)) {
        completed.push(operation.clientOperationId);
        continue;
      }
      const batch = next.batches.find((candidate) => candidate.id === operation.batchId);
      if (!batch || batch.quantity !== operation.expectedQuantity) {
        conflicts.push({
          clientOperationId: operation.clientOperationId,
          reason:
            "Tồn lô đã thay đổi sau lúc đếm. Hãy kiểm lại; thao tác cũ không được ghi đè tự động.",
        });
        continue;
      }
      next.counts.unshift({
        id: uid("KK"),
        clientOperationId: operation.clientOperationId,
        at: operation.countedAt,
        batchId: operation.batchId,
        productId: operation.productId,
        expected: operation.expectedQuantity,
        actual: operation.actualQuantity,
        note: operation.note,
        status: "REVIEW",
      });
      changed = true;
      completed.push(operation.clientOperationId);
    }

    if (changed) {
      await persist(next);
      state.value = next;
    }
    await Promise.all([
      ...completed.map((clientOperationId) => stocktakeQueue.remove(clientOperationId)),
      ...conflicts.map((conflict) =>
        stocktakeQueue.markConflict(conflict.clientOperationId, conflict.reason),
      ),
    ]);
    await refreshStocktakeQueue();

    if (conflicts.length) {
      notify(conflicts.length + " thao tác bị xung đột tồn; không có dữ liệu nào bị ghi đè.");
    } else {
      notify("Đã đồng bộ " + completed.length + " thao tác kiểm kê.");
    }
  } catch (e) {
    error.value = (e as Error).message || "Không đồng bộ được thao tác kiểm kê.";
    notify("Chưa đồng bộ được. Thao tác vẫn được giữ trên thiết bị để thử lại.");
    throw e;
  }
}

async function syncApiStocktakes(operations: StocktakeQueueItem[]) {
  if (!apiStocktakeAdapter) return;
  const session = await apiStocktakeAdapter.openSession();
  const completed: string[] = [];
  const conflicts: { clientOperationId: string; reason: string }[] = [];

  for (const operation of operations) {
    try {
      const line = await apiStocktakeAdapter.submitCount(session.id, operation);
      apiStocktakeRecords.value = [
        toStocktakeRecord(line),
        ...apiStocktakeRecords.value.filter(
          (record) => record.clientOperationId !== line.clientOperationId,
        ),
      ];
      completed.push(operation.clientOperationId);
    } catch (syncError) {
      if (
        syncError instanceof StocktakeApiError &&
        syncError.status === 409 &&
        syncError.code === "CONFLICT"
      ) {
        conflicts.push({
          clientOperationId: operation.clientOperationId,
          reason: syncError.message,
        });
        continue;
      }
      throw syncError;
    }
  }

  await Promise.all([
    ...completed.map((clientOperationId) => stocktakeQueue.remove(clientOperationId)),
    ...conflicts.map((conflict) =>
      stocktakeQueue.markConflict(conflict.clientOperationId, conflict.reason),
    ),
  ]);
  await refreshStocktakeQueue();
  if (conflicts.length) {
    notify(conflicts.length + " thao tác bị xung đột tồn; không có dữ liệu nào bị ghi đè.");
  } else {
    notify("Đã đồng bộ " + completed.length + " thao tác kiểm kê lên máy chủ.");
  }
}

function toStocktakeRecord(line: ApiStocktakeLine): StocktakeRecord {
  return {
    id: line.id,
    clientOperationId: line.clientOperationId,
    countedAt: line.countedAt,
    productId: line.productId,
    batchId: line.batchId,
    expectedQuantity: Number(line.expectedQuantity),
    actualQuantity: Number(line.actualQuantity),
    note: line.note,
    status: line.status === "PENDING" ? "REVIEW" : line.status,
  };
}
function connectionChanged() {
  online.value = navigator.onLine;
  if (connected.value && user.value) void sync().catch(() => undefined);
}
function requireLogin() {
  onTrainingReset();
  authRevision++;
  user.value = undefined;
  route.value = "dashboard";
  clearSessionMarkers();
  error.value = "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.";
  authPending.value = undefined;
  busy.value = false;
}
function toggleNetwork() {
  simulateOffline.value = !simulateOffline.value;
  if (connected.value && user.value) void sync().catch(() => undefined);
}
onMounted(async () => {
  const revision = authRevision;
  try {
    state.value = isDemoMode ? await readState() : emptyOperationalState();
  } catch {
    error.value =
      "Không mở được dữ liệu cục bộ. Hãy cho phép lưu trữ trong trình duyệt và tải lại.";
  }
  if (state.value && sessionStorage.getItem("simtim-v2-auth-mode") === authAdapter.mode) {
    try {
      const restored = await authAdapter.restoreSession();
      if (revision === authRevision) {
        user.value = restored;
        if (!restored) clearSessionMarkers();
      }
    } catch (e) {
      if (revision === authRevision) {
        clearSessionMarkers();
        error.value = (e as Error).message;
      }
    }
  } else clearSessionMarkers();
  if (revision === authRevision) authPending.value = undefined;
  if (!isTrainingRoute.value) {
    try {
      await refreshStocktakeQueue();
      if (connected.value && user.value) await sync();
    } catch (e) {
      error.value =
        (e as Error).message ||
        "Không mở được hàng đợi kiểm kê. Hãy cho phép lưu trữ trong trình duyệt.";
    }
  }
  window.addEventListener("online", connectionChanged);
  window.addEventListener("offline", connectionChanged);
  window.addEventListener("simtim:auth-required", requireLogin);
});
onUnmounted(() => {
  authRevision++;
  clearTimeout(timer);
  window.removeEventListener("online", connectionChanged);
  window.removeEventListener("offline", connectionChanged);
  window.removeEventListener("simtim:auth-required", requireLogin);
});
const query = ref("");
const category = ref("Tất cả");
const categories = computed(() => [
  "Tất cả",
  ...new Set(
    isDemoMode
      ? state.value?.products.map((p) => p.category)
      : apiSaleProducts.value.map((p) => p.category),
  ),
]);
const filtered = computed(() => {
  const products = isDemoMode
    ? (state.value?.products ?? []).map((p) => ({ ...p, sku: "" }))
    : apiSaleProducts.value.map((p) => ({ ...p, price: 0, barcode: "" }));
  return products.filter(
    (p) =>
      (category.value === "Tất cả" || p.category === category.value) &&
      `${p.name} ${p.id} ${p.barcode} ${isDemoMode ? "" : p.sku}`
        .toLocaleLowerCase("vi")
        .includes(query.value.toLocaleLowerCase("vi")),
  );
});
const scanFeedback = ref("");
function notifyScan(message: string) {
  scanFeedback.value = message;
  notify(message);
}
async function scanBarcode(value: string) {
  scanFeedback.value = "Đang tìm sản phẩm…";
  if (pendingApiCheckout.value || posCartPending.value || posCartSaving.value) {
    notifyScan("Giỏ đang chờ xác nhận. Hãy xử lý thao tác đang chờ trước khi quét tiếp.");
    return;
  }
  if (isDemoMode) {
    const match = state.value?.products.find((p) => p.barcode === value || p.id === value);
    if (match) {
      if (await add(match.id)) notifyScan("Đã thêm " + match.name + " vào giỏ.");
      else notifyScan("Chưa thêm được hàng. Hãy kiểm tra tồn và kết nối.");
    } else notifyScan("Mã này chưa có trong danh mục demo trên trình duyệt.");
    return;
  }
  if (!connected.value || !posCartReady.value)
    return notifyScan("Đợi kết nối giỏ chung trước khi quét.");
  try {
    const res = await authAdapter.fetchApi(
      `/api/v1/products/search?barcode=${encodeURIComponent(value)}`,
    );
    if (!res.ok) throw new Error("Barcode lookup failed");
    const matches = (await res.json()) as Array<{ id: string; name: string; status?: string }>;
    const active = matches.filter((p) => !p.status || p.status === "ACTIVE");
    if (active.length === 1) {
      if (await add(active[0].id)) notifyScan("Đã thêm " + active[0].name + " vào giỏ chung.");
      else notifyScan("Chưa xác nhận thêm hàng. Hãy kiểm tra trạng thái đồng bộ giỏ.");
      query.value = "";
    } else notifyScan("Không tìm thấy một sản phẩm đang bán với mã vạch này.");
  } catch {
    notifyScan("Không tra được mã vạch. Vui lòng thử lại.");
  }
}
async function addSearchMatch() {
  if (filtered.value.length === 1) {
    await add(filtered.value[0].id);
    return;
  }
  if (isDemoMode || !query.value.trim() || !connected.value) return;
  await scanBarcode(query.value.trim());
}
const cart = ref<{ id: string; quantity: number }[]>([]);
const method = ref("Tiền mặt");
const cash = ref(0);
const apiQuote = ref<QuoteResult>();
const apiQuoteStatus = ref<"idle" | "loading" | "ready" | "error">("idle");
let apiQuoteRevision = 0;

interface PendingApiCheckout extends CheckoutRequest {
  scope: string;
  idempotencyKey: string;
}

const pendingCheckoutStorageKey = "simtim-v2-pending-sale-checkout";
const pendingApiCheckout = ref<PendingApiCheckout>();
const apiCheckoutPending = computed(() => !!pendingApiCheckout.value);

function saleScope() {
  const session = user.value?.session;
  return session ? `${session.organizationId}:${session.storeId}:${session.userId}` : "";
}

const sharedCart = useSharedPosCart(
  (path, init) => authAdapter.fetchApi(path, init),
  (snapshot) => {
    if (pendingApiCheckout.value) return;
    cart.value = snapshot.items.map((item) => ({
      id: item.productId,
      quantity: Number(item.quantity),
    }));
    void refreshApiQuote();
  },
  notify,
);
const posCartReady = sharedCart.ready;
const posCartSaving = sharedCart.saving;
const posCartError = sharedCart.error;
const posCartPending = sharedCart.pending;
let activeCartScope = "";
watch(
  () => [saleScope(), route.value, isTrainingRoute.value],
  () => {
    if (!isDemoMode && activeCartScope !== saleScope()) {
      activeCartScope = saleScope();
      cart.value = [];
      pendingApiCheckout.value = undefined;
      apiQuote.value = undefined;
      apiQuoteStatus.value = "idle";
      apiQuoteRevision++;
    }
    if (!isDemoMode && saleScope() && route.value === "sale" && !isTrainingRoute.value) {
      sharedCart.start(saleScope());
    } else sharedCart.stop();
  },
);
onUnmounted(sharedCart.stop);

function currentCartItems(): CartLine[] {
  return cart.value.map((line) => ({
    productId: line.id,
    quantity: String(line.quantity),
  }));
}

function storePendingCheckout(operation?: PendingApiCheckout) {
  pendingApiCheckout.value = operation;
  if (operation) sessionStorage.setItem(pendingCheckoutStorageKey, JSON.stringify(operation));
  else sessionStorage.removeItem(pendingCheckoutStorageKey);
}

function restorePendingCheckout() {
  if (isDemoMode || !saleScope()) return;
  pendingApiCheckout.value = undefined;
  const saved = sessionStorage.getItem(pendingCheckoutStorageKey);
  if (!saved) return;
  try {
    const operation = JSON.parse(saved) as PendingApiCheckout;
    if (
      operation.scope !== saleScope() ||
      !operation.idempotencyKey ||
      !Array.isArray(operation.items)
    ) {
      return;
    }
    pendingApiCheckout.value = operation;
    cart.value = operation.items.map((line) => ({
      id: line.productId,
      quantity: Number(line.quantity),
    }));
    cash.value = operation.cashAmount;
  } catch {
    sessionStorage.removeItem(pendingCheckoutStorageKey);
  }
}

function checkoutOutcomeIsUncertain(error: SaleApiError) {
  return error.status >= 500;
}

async function refreshApiQuote() {
  if (isDemoMode || pendingApiCheckout.value) return;
  const revision = ++apiQuoteRevision;
  if (!cart.value.length) {
    apiQuote.value = undefined;
    apiQuoteStatus.value = "idle";
    return;
  }
  apiQuote.value = undefined;
  apiQuoteStatus.value = "loading";
  try {
    const quote = await saleAdapter.quote(currentCartItems());
    if (revision !== apiQuoteRevision) return;
    apiQuote.value = quote;
    apiQuoteStatus.value = "ready";
  } catch (e) {
    if (revision !== apiQuoteRevision) return;
    apiQuoteStatus.value = "error";
    notify(
      e instanceof SaleNetworkError
        ? "Lỗi mạng khi lấy báo giá. Kiểm tra kết nối và thử lại."
        : (e as Error).message,
    );
  }
}

function apiUnitPrice(productId: string) {
  return apiQuote.value?.lines.find((line) => line.productId === productId)?.unitPrice;
}

const total = computed(() => {
  if (!isDemoMode)
    return pendingApiCheckout.value?.expectedTotal ?? apiQuote.value?.grandTotal ?? 0;
  return cart.value.reduce(
    (n, l) =>
      n +
      price(
        state.value!,
        state.value!.products.find((p) => p.id === l.id)!,
      ) *
        l.quantity,
    0,
  );
});
async function add(id: string) {
  if (!connected.value) return notify("Bán hàng cần kết nối.");
  if (pendingApiCheckout.value)
    return notify("Giao dịch đang chờ đối soát. Hãy thử lại cùng mã trước khi sửa giỏ hàng.");
  if (!isDemoMode) {
    if (!posCartReady.value || posCartPending.value || posCartSaving.value)
      return notify("Đợi giỏ chung đồng bộ xong trước khi thêm hàng.");
    return sharedCart.adjust(id, 1);
  }
  if (isDemoMode) {
    const row = cart.value.find((l) => l.id === id);
    if (available(state.value!, id) < (row?.quantity || 0) + 1)
      return notify("Không đủ tồn còn hạn để bán.");
  }
  const row = cart.value.find((l) => l.id === id);
  if (row) row.quantity++;
  else cart.value.push({ id, quantity: 1 });
  return true;
}
async function quantity(id: string, delta: number) {
  if (pendingApiCheckout.value)
    return notify("Giao dịch đang chờ đối soát. Hãy thử lại cùng mã trước khi sửa giỏ hàng.");
  if (!isDemoMode) {
    await sharedCart.adjust(id, delta);
    return;
  }
  const row = cart.value.find((l) => l.id === id)!;
  if (delta > 0) add(id);
  else {
    if (row.quantity > 1) row.quantity--;
    else cart.value = cart.value.filter((l) => l.id !== id);
  }
}
async function pay() {
  if (!user.value || user.value.role === "admin") return;
  if (!isDemoMode && (posCartSaving.value || posCartPending.value || !posCartReady.value))
    return notify("Đợi giỏ chung đồng bộ xong trước khi thanh toán.");
  if (!isDemoMode && method.value !== "Tiền mặt")
    return notify("API bán hàng hiện chỉ hỗ trợ thanh toán CASH.");
  if (!isDemoMode && !pendingApiCheckout.value && apiQuoteStatus.value !== "ready")
    return notify("Chưa có báo giá hợp lệ. Hãy chờ máy chủ xác nhận tổng tiền.");
  if (method.value === "Tiền mặt" && cash.value < total.value)
    return notify("Số tiền khách đưa chưa đủ.");
  if (busy.value) return;

  if (isDemoMode) {
    // Demo mode: quote then checkout through SalePort (wraps existing logic)
    busy.value = true;
    error.value = "";
    try {
      const items = cart.value.map((l) => ({
        productId: l.id,
        quantity: String(l.quantity),
      }));
      const quoteResult = await saleAdapter.quote(items);
      const invoice = await saleAdapter.checkout({
        items,
        expectedTotal: quoteResult.grandTotal,
        cashAmount: cash.value,
      });
      notify(`Đã lưu hóa đơn ${invoice.invoiceNo} và trừ tồn kho.`);
      cart.value = [];
      cash.value = 0;
      go("invoices");
    } catch (e) {
      notify((e as Error).message);
    } finally {
      busy.value = false;
    }
    return;
  }

  // API mode: checkout the quote already shown to the cashier. An uncertain
  // network result keeps this exact payload and key for a safe replay.
  busy.value = true;
  const operation =
    pendingApiCheckout.value ??
    ({
      scope: saleScope(),
      items: currentCartItems(),
      expectedTotal: apiQuote.value!.grandTotal,
      cashAmount: cash.value,
      idempotencyKey: crypto.randomUUID(),
      cartVersion: sharedCart.snapshot.value?.version,
    } satisfies PendingApiCheckout);
  if (!pendingApiCheckout.value) storePendingCheckout(operation);
  try {
    let invoice;
    try {
      invoice = await saleAdapter.checkout(operation);
    } catch (e) {
      if (e instanceof SaleApiError) {
        if (checkoutOutcomeIsUncertain(e)) {
          throw new Error(
            `Máy chủ trung gian trả lỗi ${e.status}; kết quả thanh toán chưa chắc chắn. Giao dịch được giữ nguyên, hãy bấm thử lại để dùng đúng mã giao dịch.`,
            { cause: e },
          );
        }
        // A 4xx response definitively rejected this operation, so the cashier
        // may correct the cart/quote and start a new checkout with a new key.
        storePendingCheckout();
        if (e.code === "CART_CHANGED") {
          await sharedCart.refresh();
          const latest = sharedCart.snapshot.value;
          if (latest) {
            cart.value = latest.items.map((item) => ({
              id: item.productId,
              quantity: Number(item.quantity),
            }));
            void refreshApiQuote();
          }
          throw new Error(
            "Giỏ đã thay đổi trên thiết bị khác. Hãy kiểm tra lại và xác nhận tổng mới.",
            { cause: e },
          );
        }
        if (e.code === "PRICE_CHANGED") {
          void refreshApiQuote();
          throw new Error(
            `Giá sản phẩm vừa thay đổi. Tổng mới: ${e.message}. Vui lòng kiểm tra lại giỏ hàng.`,
            { cause: e },
          );
        }
        if (e.code === "INSUFFICIENT_STOCK") {
          void refreshApiQuote();
          throw new Error(
            "Không đủ tồn kho cho một hoặc nhiều sản phẩm. Vui lòng điều chỉnh giỏ hàng.",
            { cause: e },
          );
        }
        if (e.code === "IDEMPOTENCY_KEY_REUSED") {
          throw new Error("Mã giao dịch không khớp payload đã lưu. Vui lòng tạo giao dịch mới.", {
            cause: e,
          });
        }
      }
      if (e instanceof SaleNetworkError) {
        throw new Error(
          "Mất kết nối khi thanh toán. Giao dịch được giữ nguyên; hãy bấm thử lại để dùng đúng mã giao dịch, không tạo hóa đơn thứ hai.",
          { cause: e },
        );
      }
      throw e;
    }
    storePendingCheckout();
    notify(`Đã tạo hóa đơn ${invoice.invoiceNo} · Tiền thừa ${money(invoice.changeAmount)}`);
    cart.value = [];
    cash.value = 0;
    apiQuote.value = undefined;
    apiQuoteStatus.value = "idle";
    // Refresh invoice list then navigate
    await loadInvoices();
    go("invoices");
  } catch (e) {
    notify((e as Error).message);
  } finally {
    busy.value = false;
  }
}
const modal = ref<HTMLDialogElement>();
const dialogType = ref("");
const selectedInvoice = ref<State["invoices"][number]>();
const selectedApiInvoice = ref<ApiInvoice>();
const form = reactive({
  name: "",
  barcode: "",
  category: "Sữa & đồ uống",
  unit: "Hộp",
  price: 0,
  phone: "",
  productId: "SP001",
  percent: 10,
  end: today(),
});
function openDialog(type: string) {
  dialogType.value = type;
  error.value = "";
  Object.assign(form, { name: "", barcode: "", phone: "", price: 0 });
  modal.value?.showModal();
}
function closeDialog() {
  modal.value?.close();
  error.value = "";
}
async function submitDialog() {
  const ok = await change((s) => {
    if (user.value?.role !== "manager") throw Error("Chỉ quản lý được thay đổi danh mục.");
    if (dialogType.value === "product") {
      if (!form.name.trim() || !form.barcode.trim() || form.price <= 0)
        throw Error("Điền tên, mã vạch và giá bán lớn hơn 0.");
      if (s.products.some((p) => p.barcode === form.barcode.trim()))
        throw Error("Mã vạch đã tồn tại.");
      s.products.push({
        id: uid("SP"),
        name: form.name.trim(),
        barcode: form.barcode.trim(),
        category: form.category,
        unit: form.unit,
        price: form.price,
        emoji: "📦",
      });
    } else if (dialogType.value === "supplier") {
      if (!form.name.trim() || !form.phone.trim()) throw Error("Điền tên và số điện thoại.");
      s.suppliers.push({
        id: uid("NCC"),
        name: form.name.trim(),
        phone: form.phone.trim(),
      });
    } else if (dialogType.value === "promotion") {
      if (form.percent <= 0 || form.percent > 90 || form.end < today())
        throw Error("Mức giảm từ 1–90%, ngày kết thúc không được ở quá khứ.");
      s.promotions = s.promotions.filter((p) => p.productId !== form.productId);
      s.promotions.push({
        productId: form.productId,
        percent: form.percent,
        end: form.end,
      });
    }
  }, "Đã lưu thay đổi.");
  if (ok) closeDialog();
}
async function saveCount(submission: StocktakeSubmission) {
  const context = stocktakeRetryContext();
  if (!context?.canSync || !state.value) throw Error("Không có quyền kiểm kê.");
  const batch = isDemoMode
    ? state.value.batches.find((candidate) => candidate.id === submission.batchId)
    : undefined;
  if (isDemoMode && !batch) throw Error("Lô hàng không còn trong cửa hàng. Hãy tải lại dữ liệu.");
  const unit = isDemoMode
    ? state.value.products.find((product) => product.id === batch?.productId)?.unit
    : submission.unitCode;
  const checked = validateStocktakeQuantity(String(submission.actualQuantity), unit);
  if (checked.error || checked.value === undefined) throw Error(checked.error);

  const operation = await stocktakeQueue.enqueue({
    actorId: context.actorId,
    organizationId: context.organizationId,
    storeId: context.storeId,
    batchId: submission.batchId,
    productId: submission.productId,
    expectedQuantity: submission.expectedQuantity,
    baseVersion: submission.baseVersion,
    actualQuantity: checked.value,
    note: submission.note,
  });
  await refreshStocktakeQueue();
  if (connected.value) {
    await sync();
  } else {
    notify(
      "Đã lưu offline thao tác " +
        operation.clientOperationId +
        ". Mã này sẽ được giữ nguyên khi thử lại.",
    );
  }
}

async function approveCount(record: StocktakeRecord) {
  const approved = await change((s) => {
    if (user.value?.role !== "manager") throw Error("Chỉ quản lý được duyệt.");
    const row = s.counts.find((candidate) => candidate.id === record.id);
    if (!row) throw Error("Không tìm thấy phiếu kiểm kê.");
    if (row.status !== "REVIEW") throw Error("Phiếu không ở trạng thái chờ duyệt.");
    const batch = s.batches.find((candidate) => candidate.id === row.batchId);
    if (!batch) throw Error("Lô hàng không còn trong cửa hàng.");
    if (batch.quantity !== row.expected) {
      row.status = "CONFLICT";
      return;
    }
    s.movements.unshift({
      id: uid("BD"),
      at: new Date().toISOString(),
      productId: row.productId,
      quantity: row.actual - batch.quantity,
      kind: "Kiểm kê",
      reference: row.id,
    });
    batch.quantity = row.actual;
    row.status = "APPROVED";
  }, "Đã kiểm tra phiếu. Xem trạng thái duyệt bên dưới.");
  if (!approved) throw Error(error.value || "Không duyệt được phiếu kiểm kê.");
}
</script>

<template>
  <TrainingGame
    v-if="isTrainingRoute"
    :employee-name="user?.name || 'Học viên Sim Tím'"
    :save-status="trainingStatus"
    :save-error="trainingSaveError"
    :server-result-required="!isDemoMode"
    :saved-result="trainingSavedResult"
    :history="trainingHistory"
    @start="onTrainingStart"
    @action="onTrainingAction"
    @reset="onTrainingReset"
    @retry-save="trainingRun?.retry()"
    @exit="onTrainingExit"
  />
  <div v-else-if="!state" class="loading">
    {{ error || "Đang mở không gian cửa hàng…" }}
  </div>
  <div v-else-if="!user" class="login">
    <section class="login-story">
      <a class="brand" href="/"><img src="/icon.svg" alt="" />sim tím<span>WORKSPACE</span></a>
      <div>
        <p class="eyebrow">MỘT CỬA HÀNG. MỌI THỨ KẾT NỐI.</p>
        <h1>Chăm cửa hàng.<br />Chạm an tâm.</h1>
        <p>Từ quầy thu ngân đến từng kệ hàng,<br />một không gian để cả đội làm việc cùng nhau.</p>
        <div class="story-cards">
          <div>
            <span>↗</span><b>Bán hàng gọn gàng</b><small>Giỏ hàng, thanh toán, hóa đơn</small>
          </div>
          <div><span>▦</span><b>Tồn kho rõ ràng</b><small>Theo từng lô và hạn sử dụng</small></div>
          <div>
            <span>✦</span><b>Học việc mỗi ngày</b><small>Thực hành trong không gian riêng</small>
          </div>
        </div>
      </div>
      <small>Sim Tím · Hệ thống quản lý bán hàng & tồn kho</small>
    </section>
    <section class="login-form">
      <div class="login-inner">
        <span class="pill">BẢN TRẢI NGHIỆM 02</span>
        <h2>Chào mừng trở lại.</h2>
        <p>Đăng nhập để bắt đầu ca làm việc của bạn.</p>
        <form @submit.prevent="signIn">
          <label
            >Mã nhân viên<input v-model="credentials.id" autocomplete="username" required /></label
          ><label
            >Mật khẩu<input
              v-model="credentials.password"
              type="password"
              autocomplete="current-password"
              required
          /></label>
          <p v-if="error" role="alert" class="error">{{ error }}</p>
          <button class="primary wide" :disabled="busy || !!authPending">
            {{
              authPending === "restore"
                ? "Đang kiểm tra phiên…"
                : authPending === "logout"
                  ? "Đang đăng xuất…"
                  : busy
                    ? "Đang đăng nhập…"
                    : "Vào không gian làm việc"
            }}
            <span>→</span>
          </button>
        </form>
        <div v-if="isDemoMode" class="demo-accounts">
          <small>CHỌN TÀI KHOẢN TRẢI NGHIỆM</small
          ><button
            v-for="a in accounts"
            :key="a.id"
            :class="{ selected: credentials.id === a.id }"
            @click="
              credentials.id = a.id;
              credentials.password = 'demo123';
            "
          >
            <span>{{ roleNames[a.role] }}</span
            ><b>{{ a.id }}</b>
          </button>
          <p>
            Mật khẩu chung: <b>demo123</b>. Xác thực được mô phỏng; dữ liệu demo lưu trên thiết bị
            này.
          </p>
          <p style="margin-top: 12px">
            <button
              type="button"
              class="training-entry"
              style="
                color: var(--primary);
                font-weight: 600;
                background: none;
                border: 0;
                padding: 0;
                cursor: pointer;
              "
              @click="go('training')"
            >
              ✦ Mở ca đào tạo nghiệp vụ (Sandbox) →
            </button>
          </p>
        </div>
      </div>
    </section>
  </div>
  <TrainingGame
    v-else-if="route === 'training'"
    :employee-name="user.name"
    :save-status="trainingStatus"
    :save-error="trainingSaveError"
    :server-result-required="!isDemoMode"
    :saved-result="trainingSavedResult"
    :history="trainingHistory"
    @start="onTrainingStart"
    @action="onTrainingAction"
    @reset="onTrainingReset"
    @retry-save="trainingRun?.retry()"
    @exit="onTrainingExit"
  />
  <div v-else class="workspace">
    <aside :class="['sidebar', { open: menu }]">
      <a href="#" class="brand" @click.prevent="go('dashboard')"
        ><img src="/icon.svg" alt="" />sim tím<span>WORKSPACE</span></a
      >
      <div class="store">
        <span class="store-icon">⌂</span>
        <div><b>Cửa hàng Sim Tím</b><small>Doanh nghiệp mẫu · 01 cửa hàng</small></div>
      </div>
      <p class="nav-label">KHÔNG GIAN LÀM VIỆC</p>
      <nav>
        <button
          v-for="[id, label, icon] in allowedNav"
          :key="id"
          :class="{ active: route === id }"
          @click="go(id)"
        >
          <span>{{ icon }}</span
          >{{ label }}<i v-if="id === 'count' && pending">{{ pending }}</i>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <span class="mini-label">PHIÊN LÀM VIỆC</span><b>{{ roleNames[user.role] }}</b
        ><small>{{ isDemoMode ? "Quyền demo theo vai trò" : "Quyền do máy chủ cấp" }}</small
        ><button class="text-button" @click="signOut">Đăng xuất ↗</button>
      </div>
    </aside>
    <button v-if="menu" class="menu-overlay" aria-label="Đóng menu" @click="menu = false"></button>
    <main class="main">
      <header class="topbar">
        <button class="menu-toggle" aria-label="Mở menu" @click="menu = !menu">☰</button>
        <div class="breadcrumb">
          Không gian cửa hàng <span>/</span><b>{{ pageTitle }}</b>
        </div>
        <div class="top-actions">
          <span :class="['connection', { offline: !connected }]"
            ><i></i>{{ connected ? "Đang kết nối" : "Ngoại tuyến" }}</span
          >
          <div class="avatar">{{ user.name[0] }}</div>
          <div class="user-name">
            <b>{{ user.name }}</b
            ><small>{{ roleNames[user.role] }}</small>
          </div>
        </div>
      </header>
      <div class="demo-strip">
        <span
          ><b>PROTOTYPE</b>
          {{
            isDemoMode
              ? "Dữ liệu trên thiết bị · đồng bộ với API mô phỏng"
              : "Phiên máy chủ · dữ liệu nghiệp vụ đọc trực tiếp từ API"
          }}</span
        ><button v-if="isDemoMode" @click="toggleNetwork">
          {{ simulateOffline ? "Kết nối lại" : "Thử mất mạng" }} ↔
        </button>
      </div>
      <div v-if="isDemoMode && !connected" class="offline-banner">
        Bạn đang offline. Có thể lưu phiếu kiểm kê; bán hàng và các thay đổi khác cần kết nối.
        <b v-if="pending">{{ pending }} phiếu chờ đồng bộ.</b>
      </div>
      <div class="page">
        <template v-if="route === 'dashboard'">
          <div class="page-head">
            <div>
              <p class="eyebrow">{{ dateText }}</p>
              <h1>
                Một ngày tốt lành, {{ user.name.split(" ")[0] }}
                <span class="wave">✦</span>
              </h1>
              <p>Mọi việc trong cửa hàng, ngay trong tầm tay bạn.</p>
            </div>
            <button
              v-if="canGo(user.role === 'stock' ? 'receive' : 'sale')"
              class="primary"
              @click="go(user.role === 'stock' ? 'receive' : 'sale')"
            >
              {{ user.role === "stock" ? "↓ Nhận hàng mới" : "+ Tạo đơn bán hàng" }}
            </button>
          </div>
          <div class="metrics">
            <article>
              <div><span>Doanh thu hôm nay</span><i>↗</i></div>
              <strong>{{ money(revenue) }}</strong
              ><small>Từ các hóa đơn đã hoàn tất</small>
            </article>
            <article>
              <div><span>Đơn hàng hôm nay</span><i>▤</i></div>
              <strong>{{ sales.length.toString().padStart(2, "0") }}</strong
              ><small>Cập nhật sau mỗi lần thanh toán</small>
            </article>
            <article>
              <div><span>Sản phẩm đang bán</span><i>▦</i></div>
              <strong>{{ state.products.length.toString().padStart(2, "0") }}</strong
              ><small>{{ state.batches.length }} lô hàng đang theo dõi</small>
            </article>
            <article class="attention">
              <div><span>Lô hàng cần chú ý</span><i>!</i></div>
              <strong>{{ alerts.length.toString().padStart(2, "0") }}</strong
              ><small>Cận hạn trong 7 ngày hoặc hết hạn</small>
            </article>
          </div>
          <div class="dashboard-columns">
            <section class="card">
              <div class="card-head">
                <div>
                  <p class="eyebrow">NHỊP CỬA HÀNG</p>
                  <h2>Giao dịch mới nhất</h2>
                </div>
                <span class="pill">Hôm nay</span>
              </div>
              <div v-if="!sales.length" class="empty">
                <div class="empty-icon">▤</div>
                <h3>Ca mới, khởi đầu mới.</h3>
                <p>
                  Hóa đơn đầu tiên sẽ xuất hiện ở đây.<br />Mọi giao dịch sẽ tự động cập nhật tồn
                  kho.
                </p>
                <button v-if="canGo('sale')" class="secondary" @click="go('sale')">
                  Bắt đầu bán hàng →
                </button>
              </div>
              <div v-else class="activity-list">
                <div v-for="i in sales.slice(0, 5)" :key="i.id">
                  <span class="activity-icon">✓</span>
                  <div>
                    <b>{{ i.id }}</b
                    ><small>{{ time(i.at) }} · {{ i.method }}</small>
                  </div>
                  <strong>{{ money(i.total) }}</strong>
                </div>
              </div>
              <div class="card-foot">
                ○ Dữ liệu phản ánh các thao tác trong bản trải nghiệm này.
              </div>
            </section>
            <section class="card alert-card">
              <div class="card-head">
                <div>
                  <p class="eyebrow">CẦN CHÚ Ý</p>
                  <h2>Đừng để hàng quá hạn</h2>
                </div>
                <span class="alert-dot">{{ alerts.length }}</span>
              </div>
              <div class="alerts">
                <div v-for="b in alerts.slice(0, 3)" :key="b.id">
                  <span class="product-emoji">{{
                    state.products.find((p) => p.id === b.productId)?.emoji
                  }}</span>
                  <div>
                    <b>{{ name(b.productId) }}</b
                    ><small>{{ b.id }} · {{ b.quantity }} sản phẩm</small
                    ><span :class="['status', b.expiry < today() ? 'danger' : 'amber']"
                      >{{ b.expiry < today() ? "Đã hết hạn" : "Cận hạn" }} · {{ b.expiry }}</span
                    >
                  </div>
                </div>
              </div>
              <button v-if="canGo('inventory')" class="card-link" @click="go('inventory')">
                Kiểm tra lô hàng <span>→</span>
              </button>
            </section>
          </div>
          <section v-if="canGo('training')" class="mentor-banner">
            <div class="mentor-portrait">
              <img src="/mentor.png" alt="Chị mentor tóc cam đeo kính" />
            </div>
            <div>
              <p class="eyebrow">HỌC VIỆC CÙNG MENTOR MAI</p>
              <h2>Làm thử trước. Tự tin hơn khi lên ca.</h2>
              <p>
                Nhập vai nhân viên: gặp khách, bán hàng tại quầy và xử lý hàng hóa trong cửa hàng
                2D.
              </p>
            </div>
            <button class="secondary" @click="go('training')">
              Vào khu đào tạo <span>↗</span>
            </button>
          </section>
          <div class="section-label">ĐƯỜNG TẮT CHO BẠN</div>
          <div class="shortcuts">
            <button v-if="canGo('products')" @click="go('products')">
              <span>⌕</span>
              <div><b>Tra cứu sản phẩm</b><small>Tên, mã vạch và giá bán</small></div>
              ↗</button
            ><button v-if="canGo('inventory')" @click="go('inventory')">
              <span>▥</span>
              <div><b>Kiểm tra tồn kho</b><small>Lô hàng và hạn sử dụng</small></div>
              ↗</button
            ><button
              v-if="canGo(user.role === 'sales' ? 'invoices' : 'count')"
              @click="go(user.role === 'sales' ? 'invoices' : 'count')"
            >
              <span>☑</span>
              <div>
                <b>{{ user.role === "sales" ? "Xem hóa đơn" : "Kiểm kê trên điện thoại" }}</b
                ><small>{{
                  user.role === "sales" ? "Lịch sử thanh toán" : "Lưu phiếu cả khi offline"
                }}</small>
              </div>
              ↗
            </button>
          </div>
        </template>

        <template v-else-if="route === 'sale'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">QUẦY THU NGÂN</p>
              <h1>Một giỏ hàng, một niềm vui.</h1>
              <p>Hàng còn hạn được xuất theo lô hết hạn sớm nhất.</p>
            </div>
            <span class="pill">{{ cart.length }} mặt hàng</span>
          </div>
          <div class="pos-layout">
            <section>
              <div class="search-bar">
                <span>⌕</span
                ><input
                  v-model="query"
                  placeholder="Tìm tên hoặc nhập mã vạch…"
                  aria-label="Tìm sản phẩm bán hàng"
                  @keydown.enter="addSearchMatch"
                />
              </div>
              <div class="filters">
                <button
                  v-for="c in categories"
                  :key="c"
                  :class="{ active: category === c }"
                  @click="category = c"
                >
                  {{ c }}
                </button>
              </div>
              <div class="product-grid">
                <button
                  v-for="p in filtered"
                  :key="p.id"
                  class="product-card"
                  :disabled="
                    !connected ||
                    apiCheckoutPending ||
                    (!isDemoMode && (!posCartReady || posCartSaving || !!posCartPending)) ||
                    (isDemoMode && available(state!, p.id) === 0)
                  "
                  @click="add(p.id)"
                >
                  <div class="product-picture">
                    <span>{{ p.emoji }}</span
                    ><i v-if="isDemoMode && price(state, p) < p.price">Ưu đãi</i>
                  </div>
                  <small>{{ p.category }}</small>
                  <h3>{{ p.name }}</h3>
                  <p v-if="isDemoMode">
                    {{ money(price(state, p))
                    }}<del v-if="price(state, p) < p.price">{{ money(p.price) }}</del>
                  </p>
                  <p v-else>Thêm vào giỏ để lấy báo giá</p>
                  <footer>
                    <span v-if="isDemoMode"
                      >Còn {{ available(state!, p.id) }} {{ p.unit.toLowerCase() }}</span
                    ><span v-else>Giá và tồn do máy chủ xác nhận</span><b>＋</b>
                  </footer>
                </button>
              </div>
              <p v-if="!filtered.length" class="empty">Không tìm thấy sản phẩm phù hợp.</p>
            </section>
            <section class="card cart">
              <div class="card-head">
                <h2>Đơn hàng hiện tại</h2>
                <span>▤</span>
              </div>
              <BarcodeScanner
                :feedback="scanFeedback"
                :disabled="
                  !connected ||
                  apiCheckoutPending ||
                  (!isDemoMode && (!posCartReady || posCartSaving || !!posCartPending))
                "
                @scanned="scanBarcode"
              />
              <p v-if="!isDemoMode" role="status" class="pos-sync-status">
                {{
                  posCartSaving
                    ? "Đang lưu giỏ chung…"
                    : posCartReady
                      ? "Giỏ chung đã kết nối · Cùng nhân viên và cửa hàng"
                      : "Đang kết nối giỏ chung…"
                }}
              </p>
              <p v-else class="pos-sync-status">Demo trình duyệt: giỏ chỉ nằm trên thiết bị này.</p>
              <p v-if="posCartError" class="error" role="alert">{{ posCartError }}</p>
              <button
                v-if="!isDemoMode && (posCartPending || posCartError)"
                :disabled="posCartSaving"
                @click="sharedCart.retry"
              >
                Thử đồng bộ lại
              </button>
              <div v-if="!cart.length" class="empty">
                <div class="empty-icon">＋</div>
                <p>Chọn sản phẩm để bắt đầu.</p>
              </div>
              <div v-for="l in cart" :key="l.id" class="cart-row">
                <div>
                  <b>{{ name(l.id) }}</b
                  ><small v-if="isDemoMode">{{
                    money(
                      price(
                        state,
                        state.products.find((p) => p.id === l.id)!,
                      ),
                    )
                  }}</small
                  ><small v-else-if="apiUnitPrice(l.id) !== undefined">{{
                    money(apiUnitPrice(l.id)!)
                  }}</small
                  ><small v-else>Đang lấy báo giá…</small>
                </div>
                <div class="stepper">
                  <button
                    :aria-label="'Giảm ' + name(l.id)"
                    :disabled="
                      apiCheckoutPending ||
                      (!isDemoMode && (!posCartReady || posCartSaving || !!posCartPending))
                    "
                    @click="quantity(l.id, -1)"
                  >
                    −</button
                  ><span>{{ l.quantity }}</span
                  ><button
                    :aria-label="'Tăng ' + name(l.id)"
                    :disabled="
                      apiCheckoutPending ||
                      (!isDemoMode && (!posCartReady || posCartSaving || !!posCartPending))
                    "
                    @click="quantity(l.id, 1)"
                  >
                    +
                  </button>
                </div>
              </div>
              <div class="cart-bottom">
                <p v-if="!isDemoMode && apiQuoteStatus === 'loading'" role="status">
                  Đang lấy báo giá từ máy chủ…
                </p>
                <p v-else-if="!isDemoMode && apiQuoteStatus === 'error'" class="error" role="alert">
                  Chưa thể xác nhận giá và tồn. Hãy thay đổi giỏ hoặc thử lại.
                </p>
                <p v-if="apiCheckoutPending" class="error" role="status">
                  Chưa nhận được kết quả thanh toán. Giỏ và số tiền đang được giữ nguyên; lần thử
                  tiếp theo sẽ dùng đúng mã giao dịch.
                </p>
                <div class="total">
                  <span>Tổng thanh toán</span><b>{{ money(total) }}</b>
                </div>
                <label
                  >Phương thức<select
                    v-model="method"
                    :disabled="!isDemoMode || apiCheckoutPending"
                  >
                    <option>Tiền mặt</option>
                    <option v-if="isDemoMode">Chuyển khoản (mô phỏng)</option>
                  </select></label
                ><small v-if="!isDemoMode">API hiện chỉ hỗ trợ thanh toán CASH.</small
                ><label v-if="method === 'Tiền mặt'"
                  >Khách đưa<input
                    v-model.number="cash"
                    type="number"
                    min="0"
                    step="500"
                    placeholder="0"
                    :disabled="apiCheckoutPending"
                /></label>
                <p v-if="method === 'Tiền mặt'" class="change">
                  Tiền thừa <b>{{ money(Math.max(0, cash - total)) }}</b>
                </p>
                <button
                  class="primary wide"
                  :disabled="
                    !cart.length ||
                    !connected ||
                    busy ||
                    (!isDemoMode && (!posCartReady || posCartSaving || !!posCartPending)) ||
                    (!isDemoMode && !apiCheckoutPending && apiQuoteStatus !== 'ready')
                  "
                  @click="pay"
                >
                  {{ apiCheckoutPending ? "Thử lại cùng mã giao dịch →" : "Xác nhận thanh toán →" }}
                </button>
              </div>
            </section>
          </div></template
        >

        <CatalogWorkspace
          v-else-if="!isDemoMode && (route === 'products' || route === 'suppliers')"
          :entry="route === 'products' ? 'products' : 'suppliers'"
          :adapter="catalogAdapter"
          :can-write="!!user.session?.permissions.includes('catalog.write')"
          :online="connected"
        />

        <template v-else-if="route === 'products'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">DANH MỤC HÀNG HÓA</p>
              <h1>Sản phẩm</h1>
              <p>Một danh mục thống nhất cho bán hàng và tồn kho.</p>
            </div>
            <button
              v-if="isDemoMode && user.role === 'manager'"
              class="primary"
              @click="openDialog('product')"
            >
              ＋ Thêm sản phẩm
            </button>
          </div>
          <div class="search-bar">
            <span>⌕</span
            ><input
              v-model="query"
              :placeholder="
                isDemoMode ? 'Tìm theo tên, mã sản phẩm, mã vạch…' : 'Tìm theo tên, mã sản phẩm…'
              "
              aria-label="Tìm danh mục"
            />
          </div>
          <div class="card table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Mã vạch</th>
                  <th>Danh mục</th>
                  <th>Giá bán</th>
                  <th>Khả dụng</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in filtered" :key="p.id">
                  <td>
                    <div class="product-cell">
                      <span>{{ p.emoji }}</span>
                      <div>
                        <b>{{ p.name }}</b
                        ><small>{{ p.id }} · {{ p.unit }}</small>
                      </div>
                    </div>
                  </td>
                  <td class="mono">{{ p.barcode || "—" }}</td>
                  <td>{{ p.category }}</td>
                  <td>{{ isDemoMode ? money(p.price) : "Theo báo giá" }}</td>
                  <td>
                    <span class="status">{{
                      isDemoMode ? `${available(state!, p.id)} ${p.unit}` : "—"
                    }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!filtered.length" class="empty">Không tìm thấy sản phẩm.</div>
          </div></template
        >

        <template v-else-if="route === 'suppliers'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">ĐỐI TÁC CỬA HÀNG</p>
              <h1>Nhà cung cấp</h1>
              <p>Thông tin liên hệ cho từng lần nhận hàng.</p>
            </div>
            <button
              v-if="isDemoMode && user.role === 'manager'"
              class="primary"
              @click="openDialog('supplier')"
            >
              ＋ Thêm nhà cung cấp
            </button>
          </div>
          <div class="supplier-grid">
            <article v-for="s in state.suppliers" :key="s.id" class="card supplier">
              <span class="supplier-icon">♧</span><small>{{ s.id }}</small>
              <h2>{{ s.name }}</h2>
              <p>{{ s.phone }}</p>
              <span class="status">Đang hợp tác</span>
            </article>
          </div></template
        >

        <InventoryWorkspace
          v-else-if="route === 'receive' || route === 'inventory'"
          :view="route"
          :adapter="inventoryAdapter"
          :user-id="user.id"
          :online="connected"
          :can-receive="canConfirmReceipt"
        />

        <StocktakeWorkspace
          v-else-if="route === 'count'"
          :adapter="inventoryAdapter"
          :store-id="stocktakeStoreId"
          :records="stocktakeRecords"
          :connected="connected"
          :can-write="canWriteStocktake"
          :write-unavailable-reason="stocktakeWriteUnavailableReason"
          :save="saveCount"
          :approve="isDemoMode && user.role === 'manager' ? approveCount : undefined"
          :sync="sync"
        />

        <template v-else-if="route === 'invoices'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">LỊCH SỬ BÁN HÀNG</p>
              <h1>Hóa đơn</h1>
              <p>Giá và mức giảm được giữ tại thời điểm giao dịch.</p>
            </div>
            <button class="primary" @click="go('sale')">＋ Đơn bán hàng mới</button>
          </div>
          <!-- Demo mode: use local state -->
          <section v-if="isDemoMode" class="card">
            <div v-if="!state.invoices.length" class="empty">
              <h3>Chưa có hóa đơn nào.</h3>
              <p>Tạo một giao dịch tại quầy bán hàng để bắt đầu.</p>
            </div>
            <button
              v-for="i in state.invoices"
              :key="i.id"
              class="invoice-row"
              @click="
                selectedInvoice = i;
                openDialog('invoice');
              "
            >
              <span class="activity-icon">▤</span>
              <div>
                <b>{{ i.id }}</b
                ><small>{{ time(i.at) }} · {{ i.staff }} · {{ i.method }}</small>
              </div>
              <strong>{{ money(i.total) }}</strong
              ><span class="status">Hoàn tất</span><span>→</span>
            </button>
          </section>
          <!-- API mode: load from server -->
          <template v-else>
            <div v-if="invoicesStatus === 'loading'" class="empty-state">
              <span class="icon">⌛</span>
              <p>Đang tải danh sách hóa đơn...</p>
            </div>
            <div v-else-if="invoicesStatus === 'error'" class="empty-state">
              <span class="icon">❌</span>
              <p>Không tải được danh sách hóa đơn.</p>
              <button class="primary" @click="loadInvoices">Thử lại</button>
            </div>
            <section v-else class="card">
              <div v-if="!apiInvoices.length" class="empty">
                <h3>Chưa có hóa đơn nào.</h3>
                <p>Tạo một giao dịch tại quầy bán hàng để bắt đầu.</p>
              </div>
              <button
                v-for="i in apiInvoices"
                :key="i.id"
                class="invoice-row"
                @click="
                  selectedApiInvoice = i;
                  openDialog('api-invoice');
                "
              >
                <span class="activity-icon">▤</span>
                <div>
                  <b>{{ i.invoiceNo }}</b
                  ><small>{{ time(i.soldAt) }} · {{ i.status }}</small>
                </div>
                <strong>{{ money(i.grandTotal) }}</strong
                ><span class="status">Hoàn tất</span><span>→</span>
              </button>
            </section>
          </template></template
        >

        <PromotionWorkspace
          v-else-if="route === 'promotions' && !isDemoMode"
          :adapter="promotionAdapter"
          :can-manage="canManagePromotions"
          :online="connected"
        />

        <template v-else-if="route === 'promotions' && isDemoMode"
          ><div class="page-head">
            <div>
              <p class="eyebrow">THÊM LÝ DO ĐỂ KHÁCH GHÉ QUA</p>
              <h1>Khuyến mãi</h1>
              <p>Giảm theo sản phẩm trong thời gian hiệu lực; áp dụng tự động tại quầy.</p>
            </div>
            <button class="primary" @click="openDialog('promotion')">
              ＋ Tạo / cập nhật ưu đãi
            </button>
          </div>
          <div class="supplier-grid">
            <article v-for="p in state.promotions" :key="p.productId" class="card promotion">
              <strong>−{{ p.percent }}<span>%</span></strong>
              <h2>{{ name(p.productId) }}</h2>
              <p>Đến hết {{ p.end }}</p>
              <span :class="['status', p.end < today() ? 'danger' : '']">{{
                p.end < today() ? "Đã kết thúc" : "Đang áp dụng"
              }}</span>
            </article>
          </div></template
        >

        <template v-else-if="route === 'reports'">
          <div class="page-head">
            <div>
              <p class="eyebrow">SỐ LIỆU TỪ VẬN HÀNH</p>
              <h1>Báo cáo cửa hàng</h1>
              <p v-if="isDemoMode">Doanh thu hôm nay và tồn kho hiện tại từ dữ liệu demo.</p>
              <p v-else>Dữ liệu vận hành thực tế đã được ghi nhận vào hệ thống.</p>
            </div>
            <div style="display: flex; gap: 8px; align-items: center" v-if="!isDemoMode">
              <input type="date" v-model="reportPeriod.from" @change="loadReports" />
              <span> - </span>
              <input type="date" v-model="reportPeriod.to" @change="loadReports" />
            </div>
            <span class="pill" v-else>{{ today() }}</span>
          </div>

          <div v-if="reportStatus === 'loading'" class="empty-state">
            <span class="icon">⌛</span>
            <p>Đang tải dữ liệu báo cáo...</p>
          </div>
          <div v-else-if="reportStatus === 'error'" class="empty-state">
            <span class="icon">❌</span>
            <p>Đã xảy ra lỗi khi tải dữ liệu báo cáo.</p>
            <button class="primary" @click="loadReports">Thử lại</button>
          </div>
          <div v-else-if="reportStatus === 'empty'" class="empty-state">
            <span class="icon">📊</span>
            <p>Chưa có dữ liệu báo cáo trong khoảng thời gian này.</p>
          </div>
          <div v-else class="metrics">
            <article>
              <span>Doanh thu</span>
              <strong>{{ money(isDemoMode ? revenue : apiReport?.revenue || 0) }}</strong>
            </article>
            <article>
              <span>Hóa đơn</span>
              <strong>{{ isDemoMode ? sales.length : apiReport?.invoiceCount || 0 }}</strong>
            </article>
            <article>
              <span>Giá trị hóa đơn trung bình</span>
              <strong>{{
                money(
                  isDemoMode
                    ? sales.length
                      ? revenue / sales.length
                      : 0
                    : apiReport?.invoiceCount
                      ? apiReport.revenue / apiReport.invoiceCount
                      : 0,
                )
              }}</strong>
            </article>
            <article>
              <span>Giá trị tồn theo giá bán</span>
              <strong v-if="isDemoMode">{{
                money(
                  state?.products.reduce((n, p) => n + available(state!, p.id) * p.price, 0) || 0,
                )
              }}</strong>
              <strong v-else>Theo số lượng thực tế</strong>
            </article>
          </div>
          <div v-if="reportStatus === 'ready'" class="card form-card">
            <h2>Tồn khả dụng theo sản phẩm</h2>
            <template v-if="isDemoMode">
              <div v-for="p in state?.products" :key="p.id" class="bar-row">
                <span>{{ p.name }}</span>
                <div>
                  <i
                    :style="{
                      width:
                        Math.max(
                          1,
                          (available(state!, p.id) /
                            Math.max(
                              ...(state?.products.map((p) => available(state!, p.id)) || [1]),
                              1,
                            )) *
                            100,
                        ) + '%',
                    }"
                  ></i>
                </div>
                <b>{{ available(state!, p.id) }}</b>
              </div>
            </template>
            <template v-else>
              <div v-for="item in apiReport?.inventory" :key="item.sku" class="bar-row">
                <span
                  >{{ item.name }}
                  <span v-if="item.status === 'EXPIRED'" class="pill danger">Hết hạn</span>
                  <span v-if="item.status === 'NEAR_EXPIRY'" class="pill warning">Cận hạn</span>
                </span>
                <div>
                  <i
                    :style="{
                      width:
                        Math.max(
                          1,
                          (item.quantity /
                            Math.max(...(apiReport?.inventory.map((i) => i.quantity) || [1]), 1)) *
                            100,
                        ) + '%',
                    }"
                  ></i>
                </div>
                <b>{{ item.quantity }}</b>
              </div>
            </template>
          </div>
        </template>
      </div>
      <footer class="page-footer">
        <span>Sim Tím Workspace</span><span>Một cửa hàng mẫu · Prototype 02</span>
      </footer>
    </main>
    <button
      v-if="route !== 'training' && route !== 'count' && canGo('training')"
      class="mentor-fab"
      aria-label="Mở đào tạo cùng Mentor Mai"
      @click="go('training')"
    >
      <img src="/mentor.png" alt="" /><span>?</span>
    </button>
  </div>
  <dialog ref="modal" @close="error = ''" @click="$event.target === modal && closeDialog()">
    <div class="dialog-inner">
      <button class="dialog-close" aria-label="Đóng" @click="closeDialog">×</button
      ><template v-if="dialogType === 'invoice' && selectedInvoice"
        ><p class="eyebrow">HÓA ĐƠN BÁN HÀNG</p>
        <h2>{{ selectedInvoice.id }}</h2>
        <p>{{ time(selectedInvoice.at) }} · {{ selectedInvoice.staff }}</p>
        <div
          v-for="(l, index) in selectedInvoice.lines"
          :key="`${l.name}-${index}`"
          class="receipt-line"
        >
          <span>{{ l.name }} × {{ l.quantity }}</span
          ><b>{{ money(l.price * l.quantity) }}</b>
        </div>
        <div class="total">
          <span>Tổng cộng</span><b>{{ money(selectedInvoice.total) }}</b>
        </div>
        <p>{{ selectedInvoice.method }}</p>
        <button class="primary wide" @click="closeDialog">Đóng hóa đơn</button></template
      >
      <template v-else-if="dialogType === 'api-invoice' && selectedApiInvoice">
        <p class="eyebrow">HÓA ĐƠN BÁN HÀNG</p>
        <h2>{{ selectedApiInvoice.invoiceNo }}</h2>
        <p>{{ time(selectedApiInvoice.soldAt) }} · COMPLETED</p>
        <div v-for="(l, idx) in selectedApiInvoice.lines" :key="idx" class="receipt-line">
          <span>{{ l.productName }} × {{ l.quantity }}</span
          ><b>{{ money(l.lineTotal) }}</b>
        </div>
        <div v-if="selectedApiInvoice.discountTotal" class="receipt-line">
          <span>Giảm giá</span><b>−{{ money(selectedApiInvoice.discountTotal) }}</b>
        </div>
        <div class="total">
          <span>Tổng thanh toán</span><b>{{ money(selectedApiInvoice.grandTotal) }}</b>
        </div>
        <div class="total">
          <span>Tiền thừa</span><b>{{ money(selectedApiInvoice.changeAmount) }}</b>
        </div>
        <button class="primary wide" @click="closeDialog">Đóng hóa đơn</button>
      </template>
      <form v-else @submit.prevent="submitDialog">
        <p class="eyebrow">QUẢN LÝ DANH MỤC</p>
        <h2>
          {{
            dialogType === "product"
              ? "Thêm sản phẩm"
              : dialogType === "supplier"
                ? "Thêm nhà cung cấp"
                : "Thiết lập ưu đãi"
          }}
        </h2>
        <template v-if="dialogType === 'product'"
          ><label>Tên sản phẩm<input v-model="form.name" required /></label
          ><label>Mã vạch<input v-model="form.barcode" required /></label>
          <div class="form-row">
            <label>Danh mục<input v-model="form.category" required /></label
            ><label>Đơn vị<input v-model="form.unit" required /></label>
          </div>
          <label
            >Giá bán (đ)<input
              v-model.number="form.price"
              type="number"
              min="1"
              required /></label></template
        ><template v-if="dialogType === 'supplier'"
          ><label>Tên nhà cung cấp<input v-model="form.name" required /></label
          ><label>Điện thoại<input v-model="form.phone" type="tel" required /></label></template
        ><template v-if="dialogType === 'promotion'"
          ><label
            >Sản phẩm<select v-model="form.productId">
              <option v-for="p in state?.products" :key="p.id" :value="p.id">
                {{ p.name }}
              </option>
            </select></label
          ><label
            >Giảm giá (%)<input
              v-model.number="form.percent"
              type="number"
              min="1"
              max="90"
              required /></label
          ><label
            >Ngày kết thúc<input v-model="form.end" type="date" :min="today()" required /></label
        ></template>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="primary wide" :disabled="busy || !connected">Lưu thay đổi</button>
      </form>
    </div>
  </dialog>
  <div v-if="toast" class="toast" role="status">{{ toast }}</div>
</template>
