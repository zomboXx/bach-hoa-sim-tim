<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import TrainingGame from "./training/TrainingGame.vue";
import {
  accounts,
  available,
  checkout,
  login,
  permissions,
  persist,
  price,
  readState,
  today,
  localDay,
  uid,
  type State,
  type User,
  type Count,
} from "./api";
const state = ref<State>();
const user = ref<User>();
const route = ref("dashboard");
const error = ref("");
const toast = ref("");
let timer: ReturnType<typeof setTimeout>;
const busy = ref(false);
const menu = ref(false);
const online = ref(navigator.onLine);
const simulateOffline = ref(false);
const connected = computed(() => online.value && !simulateOffline.value);
const credentials = reactive({ id: "NV001", password: "demo123" });
const roleNames = {
  sales: "Nhân viên bán hàng",
  stock: "Nhân viên hàng hóa",
  manager: "Quản lý cửa hàng",
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
const allowedNav = computed(() =>
  nav.filter(([id]) => user.value && permissions[user.value.role].includes(id)),
);
const pageTitle = computed(() => nav.find(([id]) => id === route.value)?.[1]);
const money = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    n,
  );
const time = (s: string) =>
  new Date(s).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
  });
const name = (id: string) =>
  state.value?.products.find((p) => p.id === id)?.name || id;
const dateText = new Date().toLocaleDateString("vi-VN", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});
const sales = computed(
  () =>
    state.value?.invoices.filter((i) => localDay(new Date(i.at)) === today()) ||
    [],
);
const revenue = computed(() => sales.value.reduce((n, i) => n + i.total, 0));
const alerts = computed(
  () =>
    state.value?.batches.filter(
      (b) =>
        b.quantity > 0 &&
        Math.ceil((Date.parse(b.expiry) - Date.parse(today())) / 86400000) <= 7,
    ) || [],
);
const pending = computed(
  () => state.value?.counts.filter((c) => c.status === "PENDING").length || 0,
);
function notify(message: string) {
  toast.value = message;
  clearTimeout(timer);
  timer = setTimeout(() => (toast.value = ""), 4000);
}
function go(id: string) {
  if (!user.value || !permissions[user.value.role].includes(id)) return;
  route.value = id;
  query.value = "";
  category.value = "Tất cả";
  menu.value = false;
  error.value = "";
  window.scrollTo(0, 0);
}
function signIn() {
  try {
    if (!connected.value)
      throw Error("Cần kết nối để bắt đầu phiên đăng nhập.");
    user.value = login(credentials.id, credentials.password);
    sessionStorage.setItem("simtim-v2-user", user.value.id);
    go("dashboard");
    error.value = "";
  } catch (e) {
    error.value = (e as Error).message;
  }
}
function signOut() {
  user.value = undefined;
  sessionStorage.removeItem("simtim-v2-user");
  cart.value = [];
  error.value = "";
}
async function change(
  action: (s: State) => void,
  message: string,
  allowOffline = false,
) {
  if (busy.value || !state.value) return;
  error.value = "";
  if (!connected.value && !allowOffline) {
    notify(
      "Cần kết nối để thực hiện thao tác này. Chỉ kiểm kê hỗ trợ offline.",
    );
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
    error.value = (e as Error).message;
    notify(error.value);
    return false;
  } finally {
    busy.value = false;
  }
}
async function sync() {
  if (!connected.value || !pending.value || busy.value) return;
  await change((s) => {
    for (const c of s.counts.filter((c) => c.status === "PENDING")) {
      const b = s.batches.find((b) => b.id === c.batchId);
      c.status = b?.quantity === c.expected ? "REVIEW" : "CONFLICT";
    }
  }, "Đã đồng bộ phiếu vào dữ liệu demo trên thiết bị.");
}
function connectionChanged() {
  online.value = navigator.onLine;
  if (connected.value) void sync();
}
function toggleNetwork() {
  simulateOffline.value = !simulateOffline.value;
  if (connected.value) void sync();
}
onMounted(async () => {
  try {
    state.value = await readState();
    const saved = sessionStorage.getItem("simtim-v2-user");
    user.value = accounts.find((a) => a.id === saved);
    if (connected.value) void sync();
  } catch {
    error.value =
      "Không mở được dữ liệu cục bộ. Hãy cho phép lưu trữ trong trình duyệt và tải lại.";
  }
  window.addEventListener("online", connectionChanged);
  window.addEventListener("offline", connectionChanged);
});
onUnmounted(() => {
  clearTimeout(timer);
  window.removeEventListener("online", connectionChanged);
  window.removeEventListener("offline", connectionChanged);
});
const query = ref("");
const category = ref("Tất cả");
const categories = computed(() => [
  "Tất cả",
  ...new Set(state.value?.products.map((p) => p.category)),
]);
const filtered = computed(
  () =>
    state.value?.products.filter(
      (p) =>
        (category.value === "Tất cả" || p.category === category.value) &&
        `${p.name} ${p.id} ${p.barcode}`
          .toLocaleLowerCase("vi")
          .includes(query.value.toLocaleLowerCase("vi")),
    ) || [],
);
const cart = ref<{ id: string; quantity: number }[]>([]);
const method = ref("Tiền mặt");
const cash = ref(0);
const total = computed(() =>
  cart.value.reduce(
    (n, l) =>
      n +
      price(
        state.value!,
        state.value!.products.find((p) => p.id === l.id)!,
      ) *
        l.quantity,
    0,
  ),
);
function add(id: string) {
  if (!connected.value) return notify("Bán hàng cần kết nối.");
  const row = cart.value.find((l) => l.id === id);
  if (available(state.value!, id) < (row?.quantity || 0) + 1)
    return notify("Không đủ tồn còn hạn để bán.");
  if (row) row.quantity++;
  else cart.value.push({ id, quantity: 1 });
}
function quantity(id: string, delta: number) {
  const row = cart.value.find((l) => l.id === id)!;
  if (delta > 0) add(id);
  else if (row.quantity > 1) row.quantity--;
  else cart.value = cart.value.filter((l) => l.id !== id);
}
async function pay() {
  if (method.value === "Tiền mặt" && cash.value < total.value)
    return notify("Số tiền khách đưa chưa đủ.");
  if (
    await change((s) => {
      checkout(s, user.value!, cart.value, method.value);
    }, "Đã lưu hóa đơn và trừ tồn kho.")
  ) {
    cart.value = [];
    cash.value = 0;
    go("invoices");
  }
}
const modal = ref<HTMLDialogElement>();
const dialogType = ref("");
const selectedInvoice = ref<State["invoices"][number]>();
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
    if (user.value?.role !== "manager")
      throw Error("Chỉ quản lý được thay đổi danh mục.");
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
      if (!form.name.trim() || !form.phone.trim())
        throw Error("Điền tên và số điện thoại.");
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
const receipt = reactive({
  supplier: "NCC01",
  product: "SP001",
  lot: "",
  expiry: "",
  delivered: 20,
  accepted: 18,
  note: "",
});
async function receive() {
  const ok = await change((s) => {
    if (!permissions[user.value!.role].includes("receive"))
      throw Error("Không có quyền nhận hàng.");
    if (!receipt.lot.trim() || !receipt.expiry || receipt.expiry < today())
      throw Error("Nhập mã lô và hạn sử dụng còn hiệu lực.");
    if (s.batches.some((b) => b.id === receipt.lot.trim()))
      throw Error("Mã lô đã tồn tại.");
    if (
      !Number.isInteger(receipt.delivered) ||
      !Number.isInteger(receipt.accepted) ||
      receipt.accepted < 1 ||
      receipt.accepted > receipt.delivered
    )
      throw Error(
        "Số lượng nhận phải là số nguyên dương và không vượt số giao.",
      );
    if (receipt.accepted < receipt.delivered && !receipt.note.trim())
      throw Error("Ghi lý do từ chối hàng.");
    const id = uid("NH");
    s.batches.push({
      id: receipt.lot.trim(),
      productId: receipt.product,
      quantity: receipt.accepted,
      expiry: receipt.expiry,
    });
    s.receipts.unshift({
      id,
      supplier: receipt.supplier,
      product: receipt.product,
      accepted: receipt.accepted,
      rejected: receipt.delivered - receipt.accepted,
      note: receipt.note,
    });
    s.movements.unshift({
      id: uid("BD"),
      at: new Date().toISOString(),
      productId: receipt.product,
      quantity: receipt.accepted,
      kind: "Nhận hàng",
      reference: id,
    });
  }, "Đã nhận hàng, tạo lô và tăng tồn.");
  if (ok) {
    receipt.lot = "";
    receipt.note = "";
  }
}
const count = reactive({ batch: "LO01", actual: 0, note: "" });
const countBatch = computed(() =>
  state.value?.batches.find((b) => b.id === count.batch),
);
async function saveCount() {
  if (
    await change(
      (s) => {
        if (!permissions[user.value!.role].includes("count"))
          throw Error("Không có quyền kiểm kê.");
        if (!Number.isInteger(count.actual) || count.actual < 0)
          throw Error("Số thực tế phải là số nguyên không âm.");
        const b = s.batches.find((b) => b.id === count.batch)!;
        s.counts.unshift({
          id: uid("KK"),
          at: new Date().toISOString(),
          batchId: b.id,
          productId: b.productId,
          expected: b.quantity,
          actual: count.actual,
          note: count.note,
          status: connected.value ? "REVIEW" : "PENDING",
        });
      },
      connected.value
        ? "Đã gửi phiếu chờ quản lý duyệt."
        : "Đã lưu offline trên thiết bị. Phiếu sẽ đồng bộ khi có kết nối.",
      true,
    )
  )
    count.note = "";
}
async function approve(c: Count) {
  await change((s) => {
    if (user.value?.role !== "manager") throw Error("Chỉ quản lý được duyệt.");
    const row = s.counts.find((x) => x.id === c.id)!;
    if (row.status !== "REVIEW")
      throw Error("Phiếu không ở trạng thái chờ duyệt.");
    const b = s.batches.find((b) => b.id === row.batchId)!;
    if (b.quantity !== row.expected) {
      row.status = "CONFLICT";
      return;
    }
    s.movements.unshift({
      id: uid("BD"),
      at: new Date().toISOString(),
      productId: row.productId,
      quantity: row.actual - b.quantity,
      kind: "Kiểm kê",
      reference: row.id,
    });
    b.quantity = row.actual;
    row.status = "APPROVED";
  }, "Đã kiểm tra phiếu. Xem trạng thái duyệt bên dưới.");
}
const statuses = {
  PENDING: "Chờ kết nối",
  REVIEW: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  CONFLICT: "Tồn đã đổi · cần kiểm lại",
};
</script>

<template>
  <div v-if="!state" class="loading">
    {{ error || "Đang mở không gian cửa hàng…" }}
  </div>
  <div v-else-if="!user" class="login">
    <section class="login-story">
      <a class="brand" href="/"
        ><img src="/icon.svg" alt="" />sim tím<span>WORKSPACE</span></a
      >
      <div>
        <p class="eyebrow">MỘT CỬA HÀNG. MỌI THỨ KẾT NỐI.</p>
        <h1>Chăm cửa hàng.<br />Chạm an tâm.</h1>
        <p>
          Từ quầy thu ngân đến từng kệ hàng,<br />một không gian để cả đội làm
          việc cùng nhau.
        </p>
        <div class="story-cards">
          <div>
            <span>↗</span><b>Bán hàng gọn gàng</b
            ><small>Giỏ hàng, thanh toán, hóa đơn</small>
          </div>
          <div>
            <span>▦</span><b>Tồn kho rõ ràng</b
            ><small>Theo từng lô và hạn sử dụng</small>
          </div>
          <div>
            <span>✦</span><b>Học việc mỗi ngày</b
            ><small>Thực hành trong không gian riêng</small>
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
            >Mã nhân viên<input
              v-model="credentials.id"
              autocomplete="username"
              required /></label
          ><label
            >Mật khẩu<input
              v-model="credentials.password"
              type="password"
              autocomplete="current-password"
              required
          /></label>
          <p v-if="error" role="alert" class="error">{{ error }}</p>
          <button class="primary wide">
            Vào không gian làm việc <span>→</span>
          </button>
        </form>
        <div class="demo-accounts">
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
            Mật khẩu chung: <b>demo123</b>. Xác thực được mô phỏng; dữ liệu demo
            lưu trên thiết bị này.
          </p>
        </div>
      </div>
    </section>
  </div>
  <TrainingGame
    v-else-if="route === 'training'"
    :employee-name="user.name"
    @exit="go('dashboard')"
  />
  <div v-else class="workspace">
    <aside :class="['sidebar', { open: menu }]">
      <a href="#" class="brand" @click.prevent="go('dashboard')"
        ><img src="/icon.svg" alt="" />sim tím<span>WORKSPACE</span></a
      >
      <div class="store">
        <span class="store-icon">⌂</span>
        <div>
          <b>Cửa hàng Sim Tím</b><small>Doanh nghiệp mẫu · 01 cửa hàng</small>
        </div>
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
        <span class="mini-label">PHIÊN LÀM VIỆC</span
        ><b>{{ roleNames[user.role] }}</b
        ><small>Quyền thao tác theo vai trò</small
        ><button class="text-button" @click="signOut">Đăng xuất ↗</button>
      </div>
    </aside>
    <button
      v-if="menu"
      class="menu-overlay"
      aria-label="Đóng menu"
      @click="menu = false"
    ></button>
    <main class="main">
      <header class="topbar">
        <button class="menu-toggle" aria-label="Mở menu" @click="menu = !menu">
          ☰
        </button>
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
          ><b>PROTOTYPE</b> Dữ liệu trên thiết bị · đồng bộ với API mô
          phỏng</span
        ><button @click="toggleNetwork">
          {{ simulateOffline ? "Kết nối lại" : "Thử mất mạng" }} ↔
        </button>
      </div>
      <div v-if="!connected" class="offline-banner">
        Bạn đang offline. Có thể lưu phiếu kiểm kê; bán hàng và các thay đổi
        khác cần kết nối. <b v-if="pending">{{ pending }} phiếu chờ đồng bộ.</b>
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
              class="primary"
              @click="go(user.role === 'stock' ? 'receive' : 'sale')"
            >
              {{
                user.role === "stock" ? "↓ Nhận hàng mới" : "+ Tạo đơn bán hàng"
              }}
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
              <strong>{{
                state.products.length.toString().padStart(2, "0")
              }}</strong
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
                  Hóa đơn đầu tiên sẽ xuất hiện ở đây.<br />Mọi giao dịch sẽ tự
                  động cập nhật tồn kho.
                </p>
                <button
                  v-if="user.role !== 'stock'"
                  class="secondary"
                  @click="go('sale')"
                >
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
                    ><span
                      :class="[
                        'status',
                        b.expiry < today() ? 'danger' : 'amber',
                      ]"
                      >{{ b.expiry < today() ? "Đã hết hạn" : "Cận hạn" }} ·
                      {{ b.expiry }}</span
                    >
                  </div>
                </div>
              </div>
              <button class="card-link" @click="go('inventory')">
                Kiểm tra lô hàng <span>→</span>
              </button>
            </section>
          </div>
          <section class="mentor-banner">
            <div class="mentor-portrait">
              <img src="/mentor.png" alt="Chị mentor tóc cam đeo kính" />
            </div>
            <div>
              <p class="eyebrow">HỌC VIỆC CÙNG MENTOR MAI</p>
              <h2>Làm thử trước. Tự tin hơn khi lên ca.</h2>
              <p>
                Nhập vai nhân viên: gặp khách, bán hàng tại quầy và xử lý hàng
                hóa trong cửa hàng 2D.
              </p>
            </div>
            <button class="secondary" @click="go('training')">
              Vào khu đào tạo <span>↗</span>
            </button>
          </section>
          <div class="section-label">ĐƯỜNG TẮT CHO BẠN</div>
          <div class="shortcuts">
            <button @click="go('products')">
              <span>⌕</span>
              <div>
                <b>Tra cứu sản phẩm</b><small>Tên, mã vạch và giá bán</small>
              </div>
              ↗</button
            ><button @click="go('inventory')">
              <span>▥</span>
              <div>
                <b>Kiểm tra tồn kho</b><small>Lô hàng và hạn sử dụng</small>
              </div>
              ↗</button
            ><button @click="go(user.role === 'sales' ? 'invoices' : 'count')">
              <span>☑</span>
              <div>
                <b>{{
                  user.role === "sales"
                    ? "Xem hóa đơn"
                    : "Kiểm kê trên điện thoại"
                }}</b
                ><small>{{
                  user.role === "sales"
                    ? "Lịch sử thanh toán"
                    : "Lưu phiếu cả khi offline"
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
                  @keydown.enter="filtered.length === 1 && add(filtered[0].id)"
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
                  :disabled="!connected || available(state, p.id) === 0"
                  @click="add(p.id)"
                >
                  <div class="product-picture">
                    <span>{{ p.emoji }}</span
                    ><i v-if="price(state, p) < p.price">Ưu đãi</i>
                  </div>
                  <small>{{ p.category }}</small>
                  <h3>{{ p.name }}</h3>
                  <p>
                    {{ money(price(state, p)) }}
                    <del v-if="price(state, p) < p.price">{{
                      money(p.price)
                    }}</del>
                  </p>
                  <footer>
                    <span
                      >Còn {{ available(state, p.id) }}
                      {{ p.unit.toLowerCase() }}</span
                    ><b>＋</b>
                  </footer>
                </button>
              </div>
              <p v-if="!filtered.length" class="empty">
                Không tìm thấy sản phẩm phù hợp.
              </p>
            </section>
            <section class="card cart">
              <div class="card-head">
                <h2>Đơn hàng hiện tại</h2>
                <span>▤</span>
              </div>
              <div v-if="!cart.length" class="empty">
                <div class="empty-icon">＋</div>
                <p>Chọn sản phẩm để bắt đầu.</p>
              </div>
              <div v-for="l in cart" :key="l.id" class="cart-row">
                <div>
                  <b>{{ name(l.id) }}</b
                  ><small>{{
                    money(
                      price(
                        state,
                        state.products.find((p) => p.id === l.id)!,
                      ),
                    )
                  }}</small>
                </div>
                <div class="stepper">
                  <button
                    :aria-label="'Giảm ' + name(l.id)"
                    @click="quantity(l.id, -1)"
                  >
                    −</button
                  ><span>{{ l.quantity }}</span
                  ><button
                    :aria-label="'Tăng ' + name(l.id)"
                    @click="quantity(l.id, 1)"
                  >
                    +
                  </button>
                </div>
              </div>
              <div class="cart-bottom">
                <div class="total">
                  <span>Tổng thanh toán</span><b>{{ money(total) }}</b>
                </div>
                <label
                  >Phương thức<select v-model="method">
                    <option>Tiền mặt</option>
                    <option>Chuyển khoản (mô phỏng)</option>
                  </select></label
                ><label v-if="method === 'Tiền mặt'"
                  >Khách đưa<input
                    v-model.number="cash"
                    type="number"
                    min="0"
                    step="500"
                    placeholder="0"
                /></label>
                <p v-if="method === 'Tiền mặt'" class="change">
                  Tiền thừa <b>{{ money(Math.max(0, cash - total)) }}</b>
                </p>
                <button
                  class="primary wide"
                  :disabled="!cart.length || !connected || busy"
                  @click="pay"
                >
                  Xác nhận thanh toán →
                </button>
              </div>
            </section>
          </div></template
        >

        <template v-else-if="route === 'products'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">DANH MỤC HÀNG HÓA</p>
              <h1>Sản phẩm</h1>
              <p>Một danh mục thống nhất cho bán hàng và tồn kho.</p>
            </div>
            <button
              v-if="user.role === 'manager'"
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
              placeholder="Tìm theo tên, mã sản phẩm, mã vạch…"
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
                  <td class="mono">{{ p.barcode }}</td>
                  <td>{{ p.category }}</td>
                  <td>{{ money(p.price) }}</td>
                  <td>
                    <span class="status"
                      >{{ available(state, p.id) }} {{ p.unit }}</span
                    >
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!filtered.length" class="empty">
              Không tìm thấy sản phẩm.
            </div>
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
              v-if="user.role === 'manager'"
              class="primary"
              @click="openDialog('supplier')"
            >
              ＋ Thêm nhà cung cấp
            </button>
          </div>
          <div class="supplier-grid">
            <article
              v-for="s in state.suppliers"
              :key="s.id"
              class="card supplier"
            >
              <span class="supplier-icon">♧</span><small>{{ s.id }}</small>
              <h2>{{ s.name }}</h2>
              <p>{{ s.phone }}</p>
              <span class="status">Đang hợp tác</span>
            </article>
          </div></template
        >

        <template v-else-if="route === 'receive'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">TỪ NHÀ CUNG CẤP ĐẾN KỆ HÀNG</p>
              <h1>Nhận hàng & nhập kho</h1>
              <p>
                Chỉ ghi nhận số lượng đạt yêu cầu. Hàng từ chối được lưu cùng lý
                do.
              </p>
            </div>
          </div>
          <div class="two-columns">
            <form class="card form-card" @submit.prevent="receive">
              <h2>Phiếu nhận hàng mới</h2>
              <label
                >Nhà cung cấp<select v-model="receipt.supplier">
                  <option v-for="s in state.suppliers" :value="s.id">
                    {{ s.name }}
                  </option>
                </select></label
              ><label
                >Sản phẩm<select v-model="receipt.product">
                  <option v-for="p in state.products" :value="p.id">
                    {{ p.name }}
                  </option>
                </select></label
              >
              <div class="form-row">
                <label
                  >Mã lô<input
                    v-model="receipt.lot"
                    placeholder="Ví dụ: SUA-1409"
                    required /></label
                ><label
                  >Hạn sử dụng<input
                    v-model="receipt.expiry"
                    type="date"
                    :min="today()"
                    required
                /></label>
              </div>
              <div class="form-row">
                <label
                  >Số lượng giao<input
                    v-model.number="receipt.delivered"
                    type="number"
                    min="1"
                    required /></label
                ><label
                  >Số lượng chấp nhận<input
                    v-model.number="receipt.accepted"
                    type="number"
                    min="1"
                    :max="receipt.delivered"
                    required
                /></label>
              </div>
              <label
                >Lý do từ chối / ghi chú<textarea
                  v-model="receipt.note"
                  placeholder="Ví dụ: 2 hộp bị móp, rách bao bì"
                ></textarea></label
              ><button class="primary wide" :disabled="busy || !connected">
                Xác nhận nhận {{ receipt.accepted }} sản phẩm →
              </button>
            </form>
            <section class="card form-card">
              <h2>Phiếu nhận gần đây</h2>
              <p v-if="!state.receipts.length" class="empty">
                Chưa có phiếu nhận hàng.
              </p>
              <div v-for="r in state.receipts" :key="r.id" class="receipt-row">
                <b>{{ name(r.product) }}</b
                ><small
                  >{{ r.id }} ·
                  {{
                    state.suppliers.find((s) => s.id === r.supplier)?.name
                  }}</small
                >
                <p>
                  Đã nhận <strong>{{ r.accepted }}</strong> · Từ chối
                  {{ r.rejected }}
                </p>
                <small>{{ r.note }}</small>
              </div>
            </section>
          </div></template
        >

        <template v-else-if="route === 'inventory'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">TỒN KHO THEO TỪNG LÔ</p>
              <h1>Biết rõ hàng của mình.</h1>
              <p>Lô hết hạn được loại khỏi tồn khả dụng khi bán hàng.</p>
            </div>
            <button
              v-if="user.role !== 'sales'"
              class="primary"
              @click="go('count')"
            >
              ☑ Kiểm kê ngay
            </button>
          </div>
          <div class="card table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Sản phẩm / lô</th>
                  <th>Hạn sử dụng</th>
                  <th>Số lượng</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="b in state.batches" :key="b.id">
                  <td>
                    <b>{{ name(b.productId) }}</b
                    ><small>{{ b.id }}</small>
                  </td>
                  <td>{{ b.expiry }}</td>
                  <td>{{ b.quantity }}</td>
                  <td>
                    <span
                      :class="[
                        'status',
                        b.expiry < today()
                          ? 'danger'
                          : alerts.some((a) => a.id === b.id)
                            ? 'amber'
                            : '',
                      ]"
                      >{{
                        b.expiry < today()
                          ? "Hết hạn · không bán"
                          : alerts.some((a) => a.id === b.id)
                            ? "Cận hạn"
                            : "Còn hạn"
                      }}</span
                    >
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <section class="card movements">
            <div class="card-head">
              <h2>Lịch sử biến động</h2>
              <span>{{ state.movements.length }} thao tác</span>
            </div>
            <div v-if="!state.movements.length" class="empty">
              Nhận hàng, bán hàng và kiểm kê được duyệt sẽ xuất hiện ở đây.
            </div>
            <div
              v-for="m in state.movements.slice(0, 15)"
              :key="m.id"
              class="movement-row"
            >
              <div>
                <b>{{ name(m.productId) }}</b
                ><small
                  >{{ m.kind }} · {{ m.reference }} · {{ time(m.at) }}</small
                >
              </div>
              <strong :class="m.quantity > 0 ? 'positive' : 'negative'"
                >{{ m.quantity > 0 ? "+" : "" }}{{ m.quantity }}</strong
              >
            </div>
          </section></template
        >

        <template v-else-if="route === 'count'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">KIỂM HÀNG NGAY TẠI KỆ</p>
              <h1>Đếm thực tế. Ghi chính xác.</h1>
              <p>
                Lưu trên thiết bị khi offline, gửi lại khi có mạng. Tồn chỉ thay
                đổi sau khi quản lý duyệt.
              </p>
            </div>
            <button
              class="secondary"
              :disabled="!connected || !pending || busy"
              @click="sync"
            >
              ↻ Đồng bộ {{ pending }} phiếu
            </button>
          </div>
          <div class="two-columns">
            <form class="card form-card" @submit.prevent="saveCount">
              <h2>Ghi nhận kiểm kê</h2>
              <label
                >Chọn lô hàng<select v-model="count.batch">
                  <option v-for="b in state.batches" :value="b.id">
                    {{ name(b.productId) }} · {{ b.id }}
                  </option>
                </select></label
              >
              <div class="count-summary">
                <span
                  >Tồn hệ thống<b>{{ countBatch?.quantity }}</b></span
                ><span
                  >Chênh lệch<b
                    :class="
                      count.actual === (countBatch?.quantity || 0)
                        ? ''
                        : 'negative'
                    "
                    >{{ count.actual - (countBatch?.quantity || 0) }}</b
                  ></span
                >
              </div>
              <label
                >Số lượng thực tế<input
                  v-model.number="count.actual"
                  class="large-input"
                  type="number"
                  inputmode="numeric"
                  min="0"
                  required /></label
              ><label
                >Ghi chú<textarea
                  v-model="count.note"
                  placeholder="Ghi chú tình trạng hàng hoặc chênh lệch…"
                ></textarea></label
              ><button class="primary wide" :disabled="busy">
                {{ connected ? "Gửi phiếu kiểm kê" : "Lưu phiếu offline" }} →
              </button>
            </form>
            <section class="card form-card">
              <h2>Phiếu trên thiết bị</h2>
              <p v-if="!state.counts.length" class="empty">
                Chưa có phiếu kiểm kê.
              </p>
              <article v-for="c in state.counts" :key="c.id" class="count-row">
                <div>
                  <b>{{ name(c.productId) }}</b
                  ><span
                    :class="[
                      'status',
                      c.status === 'CONFLICT'
                        ? 'danger'
                        : c.status === 'APPROVED'
                          ? ''
                          : 'amber',
                    ]"
                    >{{ statuses[c.status] }}</span
                  >
                </div>
                <small>{{ c.batchId }} · {{ time(c.at) }}</small>
                <p>
                  Hệ thống {{ c.expected }} → Thực tế <b>{{ c.actual }}</b>
                </p>
                <small>{{ c.note }}</small
                ><button
                  v-if="user.role === 'manager' && c.status === 'REVIEW'"
                  class="secondary"
                  :disabled="!connected || busy"
                  @click="approve(c)"
                >
                  Duyệt điều chỉnh tồn
                </button>
                <p v-if="c.status === 'CONFLICT'" class="error">
                  Tồn lô đã thay đổi sau lúc đếm. Tạo phiếu kiểm lại lô này;
                  không ghi đè tự động.
                </p>
              </article>
            </section>
          </div></template
        >

        <template v-else-if="route === 'invoices'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">LỊCH SỬ BÁN HÀNG</p>
              <h1>Hóa đơn</h1>
              <p>Giá và mức giảm được giữ tại thời điểm giao dịch.</p>
            </div>
            <button class="primary" @click="go('sale')">
              ＋ Đơn bán hàng mới
            </button>
          </div>
          <section class="card">
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
                ><small
                  >{{ time(i.at) }} · {{ i.staff }} · {{ i.method }}</small
                >
              </div>
              <strong>{{ money(i.total) }}</strong
              ><span class="status">Hoàn tất</span><span>→</span>
            </button>
          </section></template
        >

        <template v-else-if="route === 'promotions'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">THÊM LÝ DO ĐỂ KHÁCH GHÉ QUA</p>
              <h1>Khuyến mãi</h1>
              <p>
                Giảm theo sản phẩm trong thời gian hiệu lực; áp dụng tự động tại
                quầy.
              </p>
            </div>
            <button class="primary" @click="openDialog('promotion')">
              ＋ Tạo / cập nhật ưu đãi
            </button>
          </div>
          <div class="supplier-grid">
            <article
              v-for="p in state.promotions"
              :key="p.productId"
              class="card promotion"
            >
              <strong>−{{ p.percent }}<span>%</span></strong>
              <h2>{{ name(p.productId) }}</h2>
              <p>Đến hết {{ p.end }}</p>
              <span :class="['status', p.end < today() ? 'danger' : '']">{{
                p.end < today() ? "Đã kết thúc" : "Đang áp dụng"
              }}</span>
            </article>
          </div></template
        >

        <template v-else-if="route === 'reports'"
          ><div class="page-head">
            <div>
              <p class="eyebrow">SỐ LIỆU TỪ VẬN HÀNH</p>
              <h1>Báo cáo cửa hàng</h1>
              <p>Doanh thu hôm nay và tồn kho hiện tại từ dữ liệu demo.</p>
            </div>
            <span class="pill">{{ today() }}</span>
          </div>
          <div class="metrics">
            <article>
              <span>Doanh thu hôm nay</span
              ><strong>{{ money(revenue) }}</strong>
            </article>
            <article>
              <span>Hóa đơn</span><strong>{{ sales.length }}</strong>
            </article>
            <article>
              <span>Giá trị hóa đơn trung bình</span
              ><strong>{{
                money(sales.length ? revenue / sales.length : 0)
              }}</strong>
            </article>
            <article>
              <span>Giá trị tồn theo giá bán</span
              ><strong>{{
                money(
                  state.products.reduce(
                    (n, p) => n + available(state!, p.id) * p.price,
                    0,
                  ),
                )
              }}</strong>
            </article>
          </div>
          <div class="card form-card">
            <h2>Tồn khả dụng theo sản phẩm</h2>
            <div v-for="p in state.products" class="bar-row">
              <span>{{ p.name }}</span>
              <div>
                <i
                  :style="{
                    width:
                      Math.max(
                        1,
                        (available(state, p.id) /
                          Math.max(
                            ...state.products.map((p) =>
                              available(state!, p.id),
                            ),
                            1,
                          )) *
                          100,
                      ) + '%',
                  }"
                ></i>
              </div>
              <b>{{ available(state, p.id) }}</b>
            </div>
          </div></template
        >
      </div>
      <footer class="page-footer">
        <span>Sim Tím Workspace</span
        ><span>Một cửa hàng mẫu · Prototype 02</span>
      </footer>
    </main>
    <button
      v-if="route !== 'training'"
      class="mentor-fab"
      aria-label="Mở đào tạo cùng Mentor Mai"
      @click="go('training')"
    >
      <img src="/mentor.png" alt="" /><span>?</span>
    </button>
  </div>
  <dialog
    ref="modal"
    @close="error = ''"
    @click="$event.target === modal && closeDialog()"
  >
    <div class="dialog-inner">
      <button class="dialog-close" aria-label="Đóng" @click="closeDialog">
        ×</button
      ><template v-if="dialogType === 'invoice' && selectedInvoice"
        ><p class="eyebrow">HÓA ĐƠN BÁN HÀNG</p>
        <h2>{{ selectedInvoice.id }}</h2>
        <p>{{ time(selectedInvoice.at) }} · {{ selectedInvoice.staff }}</p>
        <div v-for="l in selectedInvoice.lines" class="receipt-line">
          <span>{{ l.name }} × {{ l.quantity }}</span
          ><b>{{ money(l.price * l.quantity) }}</b>
        </div>
        <div class="total">
          <span>Tổng cộng</span><b>{{ money(selectedInvoice.total) }}</b>
        </div>
        <p>{{ selectedInvoice.method }}</p>
        <button class="primary wide" @click="closeDialog">
          Đóng hóa đơn
        </button></template
      >
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
          ><label
            >Điện thoại<input
              v-model="form.phone"
              type="tel"
              required /></label></template
        ><template v-if="dialogType === 'promotion'"
          ><label
            >Sản phẩm<select v-model="form.productId">
              <option v-for="p in state?.products" :value="p.id">
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
            >Ngày kết thúc<input
              v-model="form.end"
              type="date"
              :min="today()"
              required /></label
        ></template>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="primary wide" :disabled="busy || !connected">
          Lưu thay đổi
        </button>
      </form>
    </div>
  </dialog>
  <div v-if="toast" class="toast" role="status">{{ toast }}</div>
</template>
