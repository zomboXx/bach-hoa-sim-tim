<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import {
  ApiTransferAdapter,
  type TransferCreation,
  type TransferFetch,
  type TransferOrder,
  type TransferConfig,
  type TransferHistory,
} from "../infrastructure/api-transfer-adapter";
import { SaleApiError } from "../domain/sale";

const props = defineProps<{
  fetchApi: TransferFetch;
  scope: string;
  selected: boolean;
  cartVersion?: number;
  expectedTotal: number;
  canCreate: boolean;
  pendingTransferId?: string | null;
  historyOnly?: boolean;
}>();
const emit = defineEmits<{
  lock: [locked: boolean];
  changed: [];
  completed: [];
  amount: [amount: number | undefined];
}>();
const api = new ApiTransferAdapter((path, init) => props.fetchApi(path, init));
const config = ref<TransferConfig>();
const order = ref<TransferOrder>();
const history = ref<TransferHistory>();
const historyOpen = ref(false);
const page = ref(0);
const busy = ref(false);
const error = ref("");
const qrError = ref("");
const reason = ref("");
const qr = ref<HTMLElement>();
const pendingCreation = ref<TransferCreation>();
const now = ref(Date.now());
let disposed = false;
let polling = false;
let revision = 0;
let interval: ReturnType<typeof setInterval> | undefined;
const storageKey = () => "simtim-transfer-create:" + props.scope;
const pending = computed(() => order.value?.status === "PENDING");
const expiredLocally = computed(
  () => !!order.value && now.value >= Date.parse(order.value.expiresAt),
);
const statusText = {
  PENDING: "Chờ xác nhận tiền về",
  COMPLETED: "Đã nhận tiền",
  CANCELLED: "Đã hủy",
  EXPIRED: "Đã hết hạn",
};
const money = (value: number) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
const time = (value: string) =>
  new Date(value).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });

function saveCreation(value?: TransferCreation) {
  pendingCreation.value = value;
  if (value) sessionStorage.setItem(storageKey(), JSON.stringify(value));
  else sessionStorage.removeItem(storageKey());
  emit("lock", !!value || busy.value);
}
function accept(next: TransferOrder) {
  const previous = order.value;
  order.value = next;
  if (previous?.id !== next.id) {
    reason.value = "";
  }
  if (next.id === props.pendingTransferId || pendingCreation.value) saveCreation();
  emit(
    "amount",
    next.status === "PENDING" && next.id === props.pendingTransferId ? next.amount : undefined,
  );
  if (previous?.status === "PENDING" && next.status !== "PENDING") {
    emit("changed");
    if (next.status === "COMPLETED") emit("completed");
    void loadHistory();
  }
}
async function loadHistory() {
  if (!props.historyOnly) return;
  const current = revision;
  try {
    const result = await api.history(page.value);
    if (!disposed && current === revision) history.value = result;
  } catch (failure) {
    if (!disposed && current === revision) error.value = (failure as Error).message;
  }
}
function toggleHistory(event: Event) {
  historyOpen.value = (event.currentTarget as HTMLDetailsElement).open;
  if (historyOpen.value) void loadHistory();
}
async function load(id: string) {
  const current = revision;
  try {
    const result = await api.get(id);
    if (!disposed && current === revision) {
      accept(result);
      error.value = "";
    }
  } catch (failure) {
    if (!disposed && current === revision) error.value = (failure as Error).message;
  }
}
async function setup() {
  const current = ++revision;
  order.value = undefined;
  config.value = undefined;
  history.value = undefined;
  pendingCreation.value = undefined;
  page.value = 0;
  error.value = "";
  emit("amount", undefined);
  if (props.historyOnly) return;
  try {
    const raw = sessionStorage.getItem(storageKey());
    if (raw) {
      const parsed = JSON.parse(raw) as TransferCreation;
      if (
        typeof parsed.operationId !== "string" ||
        !Number.isInteger(parsed.cartVersion) ||
        !Number.isSafeInteger(parsed.expectedTotal)
      )
        throw new Error("Invalid saved transfer");
      pendingCreation.value = parsed;
    }
  } catch {
    sessionStorage.removeItem(storageKey());
  }
  emit("lock", !!pendingCreation.value);
  try {
    const result = await api.config();
    if (!disposed && current === revision) config.value = result;
  } catch (failure) {
    if (!disposed && current === revision) error.value = (failure as Error).message;
  }
  if (disposed || current !== revision) return;
  await loadHistory();
  if (props.pendingTransferId) await load(props.pendingTransferId);
}
async function create() {
  if (
    busy.value ||
    (!pendingCreation.value && (!props.canCreate || props.cartVersion === undefined))
  )
    return;
  busy.value = true;
  emit("lock", true);
  error.value = "";
  const current = revision;
  try {
    if (!pendingCreation.value)
      saveCreation({
        operationId: crypto.randomUUID(),
        cartVersion: props.cartVersion!,
        expectedTotal: props.expectedTotal,
      });
    const result = await api.create(pendingCreation.value!);
    if (disposed || current !== revision) return;
    accept(result);
    emit("changed");
    await loadHistory();
  } catch (failure) {
    if (disposed || current !== revision) return;
    if (failure instanceof SaleApiError && failure.status >= 400 && failure.status < 500) {
      saveCreation();
      emit("changed");
    }
    error.value = (failure as Error).message;
  } finally {
    if (!disposed && current === revision) {
      busy.value = false;
      emit("lock", !!pendingCreation.value);
    }
  }
}
async function resolve(action: "confirm" | "cancel") {
  if (!order.value || busy.value) return;
  busy.value = true;
  error.value = "";
  const current = revision;
  try {
    const result =
      action === "confirm"
        ? await api.confirmManual(order.value.id)
        : await api.cancel(order.value.id, reason.value);
    if (disposed || current !== revision) return;
    accept(result);
    emit("changed");
    await loadHistory();
  } catch (failure) {
    if (!disposed && current === revision) error.value = (failure as Error).message;
  } finally {
    if (!disposed && current === revision) {
      busy.value = false;
      emit("lock", !!pendingCreation.value);
    }
  }
}
async function poll() {
  now.value = Date.now();
  if (polling || busy.value || !pending.value || disposed) return;
  polling = true;
  try {
    await load(order.value!.id);
  } finally {
    polling = false;
  }
}
watch(
  () => props.scope,
  () => void setup(),
);
watch(
  () => props.pendingTransferId,
  (id) => {
    if (id) void load(id);
    else if (order.value?.status === "PENDING") void load(order.value.id);
  },
);
watch(
  () => [order.value?.qrPayload, qr.value, expiredLocally.value],
  async () => {
    const container = qr.value;
    const payload = order.value?.qrPayload;
    container?.replaceChildren();
    qrError.value = "";
    if (!container || !payload || expiredLocally.value) return;
    try {
      const { BrowserQRCodeSvgWriter } = await import("@zxing/browser");
      if (
        !disposed &&
        qr.value === container &&
        order.value?.qrPayload === payload &&
        !expiredLocally.value
      ) {
        container.replaceChildren(new BrowserQRCodeSvgWriter().write(payload, 256, 256));
      }
    } catch {
      qrError.value = "Chưa hiển thị được QR. Kiểm tra kết nối rồi mở lại màn hình.";
    }
  },
  { flush: "post" },
);
onMounted(() => {
  void setup();
  interval = setInterval(() => void poll(), 2000);
});
onUnmounted(() => {
  disposed = true;
  revision++;
  clearInterval(interval);
});
</script>

<template>
  <section
    class="transfer-payment"
    :aria-label="historyOnly ? 'Lịch sử chuyển khoản' : 'Thanh toán chuyển khoản'"
  >
    <template
      v-if="!historyOnly && (selected || pendingCreation) && !pendingTransferId && !pending"
    >
      <p v-if="!config" role="status">Đang đọc cấu hình chuyển khoản…</p>
      <p v-else-if="!config.enabled && !pendingCreation" role="status">
        Cửa hàng chưa cấu hình tài khoản nhận chuyển khoản. Bạn vẫn có thể thanh toán tiền mặt.
      </p>
      <template v-else>
        <p>QR chuyển khoản · Nhân viên kiểm tra tiền về và xác nhận thủ công.</p>
        <button
          class="primary wide"
          :disabled="busy || (!pendingCreation && (!canCreate || expectedTotal <= 0))"
          @click="create"
        >
          {{
            busy ? "Đang tạo…" : pendingCreation ? "Thử lại tạo QR cùng mã" : "Tạo QR và giữ hàng"
          }}
        </button>
      </template>
    </template>
    <p v-if="pendingCreation" role="status">
      Đang chờ kết quả tạo QR. Giữ nguyên thao tác để thử lại, tránh tạo đơn trùng.
    </p>
    <p v-if="pendingTransferId && !order" role="status">
      Giỏ đang khóa để chờ chuyển khoản. Đang tải giao dịch…
    </p>
    <p
      v-if="error && (selected || pendingTransferId || pendingCreation || historyOpen)"
      class="error"
      role="alert"
    >
      {{ error }}
    </p>
    <button
      v-if="pendingTransferId && order?.id !== pendingTransferId"
      @click="load(pendingTransferId)"
    >
      Quay lại giao dịch của giỏ
    </button>
    <article v-if="order" class="transfer-detail" aria-label="Chi tiết chuyển khoản">
      <h3>{{ statusText[order.status] }}</h3>
      <p>
        <strong>{{ money(order.amount) }}</strong> · {{ order.reference }}
      </p>
      <p>
        {{ order.bankBin === "970436" ? "Vietcombank" : "Ngân hàng BIN " + order.bankBin }} ·
        {{ order.accountNumber }}<br />{{ order.accountName }}
      </p>
      <template v-if="pending && !historyOnly">
        <div
          v-if="!expiredLocally"
          ref="qr"
          class="transfer-qr"
          role="img"
          :aria-label="'QR chuyển khoản ' + order.reference"
        ></div>
        <p v-if="qrError" class="error" role="alert">{{ qrError }}</p>
        <p>
          Nội dung chuyển khoản: <strong>{{ order.reference }}</strong>
        </p>
        <p>Hết hạn: {{ time(order.expiresAt) }} (giờ Việt Nam).</p>
        <p v-if="expiredLocally" role="status">
          QR đã hết hạn. Đang đối chiếu trạng thái; không chuyển thêm tiền.
        </p>
        <p>
          Quét QR không tự xác nhận thanh toán. Kiểm tra đúng người nhận, số tiền và nội dung trong
          ứng dụng ngân hàng.
        </p>
        <button
          v-if="!expiredLocally"
          class="primary wide"
          :disabled="busy"
          @click="resolve('confirm')"
        >
          {{ busy ? "Đang xác nhận…" : "Xác nhận đã nhận tiền" }}
        </button>
        <details>
          <summary>Hủy giao dịch chờ</summary>
          <p>
            Chỉ hủy sau khi kiểm tra chưa nhận tiền. Tiền đến muộn phải được đối chiếu riêng, không
            tự hoàn tiền.
          </p>
          <form @submit.prevent="resolve('cancel')">
            <label
              >Lý do hủy<input v-model="reason" maxlength="500" required :disabled="busy"
            /></label>
            <button :disabled="busy || !reason.trim()">Hủy QR và trả hàng giữ</button>
          </form>
        </details>
      </template>
      <template v-else-if="historyOnly && pending">
        <p>Giao dịch đang chờ thanh toán. Mở Bán hàng để xử lý giỏ hiện tại.</p>
      </template>
      <template v-else>
        <p>{{ order.resolutionNote }}</p>
        <p v-if="order.bankReference">Mã ngân hàng: {{ order.bankReference }}</p>
        <p v-if="order.resolvedAt">
          Xử lý lúc {{ time(order.resolvedAt)
          }}<span v-if="order.resolvedBy"> · Nhân viên: {{ order.resolvedBy }}</span>
        </p>
        <p v-if="order.status !== 'COMPLETED'">
          Nếu tiền đến sau khi hủy/hết hạn, cần đối chiếu riêng trước khi tạo đơn mới.
        </p>
      </template>
    </article>
    <details v-if="historyOnly" class="transfer-history" @toggle="toggleHistory">
      <summary>Lịch sử chuyển khoản</summary>
      <p v-if="history">
        Toàn bộ lịch sử được phép xem: {{ history.summary.completed }} đã nhận ·
        {{ history.summary.pending }} chờ · {{ history.summary.cancelled }} hủy ·
        {{ history.summary.expired }} hết hạn.<br />Tổng đã nhận:
        <strong>{{ money(history.summary.receivedAmount) }}</strong>
      </p>
      <button :disabled="busy" @click="loadHistory">Tải lại lịch sử</button>
      <ul v-if="history?.items.length">
        <li v-for="item in history.items" :key="item.id">
          <button @click="load(item.id)">
            {{ item.reference }} · {{ money(item.amount) }}<br />{{ statusText[item.status] }} ·
            {{ time(item.createdAt) }}
          </button>
        </li>
      </ul>
      <p v-else>Chưa có giao dịch trong trang này.</p>
      <div class="transfer-pages">
        <button
          :disabled="page === 0"
          @click="
            page--;
            loadHistory();
          "
        >
          Trước</button
        ><span>Trang {{ page + 1 }}</span
        ><button
          :disabled="!history || history.items.length < 10"
          @click="
            page++;
            loadHistory();
          "
        >
          Sau
        </button>
      </div>
    </details>
  </section>
</template>

<style scoped>
.transfer-payment {
  margin-top: 1rem;
  overflow-wrap: anywhere;
}
.transfer-detail {
  border: 1px solid #ddd2ef;
  border-radius: 12px;
  padding: 1rem;
  margin: 1rem 0;
}
.transfer-qr {
  background: white;
  padding: 12px;
  margin: 1rem auto;
  max-width: 280px;
}
.transfer-qr :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}
.receipt-check {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 12px 0;
}
.receipt-check input {
  width: auto;
  flex: none;
  margin-top: 4px;
}
.transfer-history {
  margin-top: 1rem;
}
.transfer-history ul {
  padding: 0;
  list-style: none;
}
.transfer-history li button {
  text-align: left;
  width: 100%;
  margin-bottom: 8px;
  white-space: normal;
}
.transfer-pages {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}
</style>
