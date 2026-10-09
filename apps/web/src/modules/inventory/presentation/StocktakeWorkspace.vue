<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import type { InventoryPort, InventorySnapshot } from "../domain/inventory";
import {
  quantityScale,
  validateStocktakeQuantity,
  type StocktakeRecord,
  type StocktakeSubmission,
  type StocktakeStatus,
} from "../domain/stocktake";

const props = defineProps<{
  adapter: InventoryPort;
  storeId: string;
  records: StocktakeRecord[];
  connected: boolean;
  canWrite: boolean;
  writeUnavailableReason?: string;
  save?: (submission: StocktakeSubmission) => Promise<void>;
  approve?: (record: StocktakeRecord) => Promise<void>;
  sync?: () => Promise<void>;
}>();

const inventory = ref<InventorySnapshot>({ products: [], batches: [], movements: [] });
const loading = ref(true);
const pageError = ref("");
const formError = ref("");
const statusMessage = ref("");
const selectedBatchId = ref("");
const actualQuantity = ref("0");
const note = ref("");
const submitting = ref(false);
const syncing = ref(false);
const approvingId = ref("");

const productById = computed(
  () => new Map(inventory.value.products.map((product) => [product.productId, product])),
);
const selectedBatch = computed(() =>
  inventory.value.batches.find((batch) => batch.batchId === selectedBatchId.value),
);
const selectedProduct = computed(() =>
  selectedBatch.value ? productById.value.get(selectedBatch.value.productId) : undefined,
);
const unitCode = computed(() => selectedProduct.value?.unitCode);
const validation = computed(() => validateStocktakeQuantity(actualQuantity.value, unitCode.value));
const difference = computed(() => {
  if (validation.value.value === undefined || !selectedBatch.value) return undefined;
  return validation.value.value - Number(selectedBatch.value.onHandQuantity);
});
const pendingCount = computed(
  () => props.records.filter((record) => record.status === "PENDING").length,
);
const inputHint = computed(() =>
  quantityScale(unitCode.value) === 3
    ? "Có thể nhập số thập phân, tối đa 3 chữ số sau dấu phẩy."
    : "Đơn vị đếm chỉ nhận số nguyên không âm.",
);

watch(selectedBatchId, () => {
  formError.value = "";
  statusMessage.value = "";
});

onMounted(loadInventory);

async function loadInventory() {
  loading.value = true;
  pageError.value = "";
  try {
    inventory.value = await props.adapter.loadInventory();
    if (
      !selectedBatchId.value ||
      !inventory.value.batches.some((batch) => batch.batchId === selectedBatchId.value)
    ) {
      selectedBatchId.value = inventory.value.batches[0]?.batchId || "";
    }
  } catch (error) {
    pageError.value = (error as Error).message || "Không tải được dữ liệu lô của cửa hàng.";
  } finally {
    loading.value = false;
  }
}

async function submit() {
  formError.value = "";
  statusMessage.value = "";
  if (!props.canWrite || !props.save) {
    formError.value =
      props.writeUnavailableReason || "Bạn không có quyền ghi nhận kiểm kê tại cửa hàng này.";
    return;
  }
  if (!selectedBatch.value) {
    formError.value = "Chọn một lô hàng để kiểm kê.";
    return;
  }
  if (validation.value.error || validation.value.value === undefined) {
    formError.value = validation.value.error || "Số lượng thực tế không hợp lệ.";
    return;
  }

  submitting.value = true;
  try {
    await props.save({
      batchId: selectedBatch.value.batchId,
      actualQuantity: validation.value.value,
      note: note.value.trim(),
    });
    note.value = "";
    statusMessage.value = props.connected
      ? "Đã gửi số đếm và chuyển sang trạng thái chờ duyệt."
      : "Đã lưu số đếm trên thiết bị; trạng thái đang chờ kết nối.";
    await loadInventory();
  } catch (error) {
    formError.value = (error as Error).message || "Không lưu được số đếm. Hãy thử lại.";
  } finally {
    submitting.value = false;
  }
}

async function syncPending() {
  if (!props.sync || !props.connected || syncing.value || pendingCount.value === 0) return;
  syncing.value = true;
  pageError.value = "";
  statusMessage.value = "";
  try {
    await props.sync();
    statusMessage.value = "Đã kiểm tra các phiếu chờ kết nối.";
  } catch (error) {
    pageError.value = (error as Error).message || "Không đồng bộ được phiếu kiểm kê.";
  } finally {
    syncing.value = false;
  }
}

async function approveRecord(record: StocktakeRecord) {
  if (!props.approve || approvingId.value) return;
  approvingId.value = record.id;
  pageError.value = "";
  statusMessage.value = "";
  try {
    await props.approve(record);
    statusMessage.value = "Đã kiểm tra trạng thái duyệt của phiếu.";
    await loadInventory();
  } catch (error) {
    pageError.value = (error as Error).message || "Không duyệt được phiếu kiểm kê.";
  } finally {
    approvingId.value = "";
  }
}

function formatQuantity(value: number | string, productId?: string) {
  const product = productId ? productById.value.get(productId) : selectedProduct.value;
  return new Intl.NumberFormat("vi-VN", {
    maximumFractionDigits: quantityScale(product?.unitCode),
  }).format(Number(value));
}

function productName(productId: string) {
  return productById.value.get(productId)?.productName || productId;
}

function productUnit(productId: string) {
  return productById.value.get(productId)?.unitCode || "đơn vị";
}

function formatTime(value: string) {
  return new Date(value).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
  });
}

const statusLabels: Record<StocktakeStatus, string> = {
  PENDING: "Chờ kết nối",
  REVIEW: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  CONFLICT: "Tồn đã đổi · cần kiểm lại",
};
</script>

<template>
  <section class="stocktake-workspace" aria-labelledby="stocktake-title">
    <header class="stocktake-header">
      <div>
        <h1 id="stocktake-title">Kiểm kê theo lô</h1>
        <p>Đối chiếu số hệ thống với số đếm thực tế ngay tại kệ hàng.</p>
      </div>
      <div class="stocktake-context" aria-label="Phạm vi kiểm kê">
        <span>Cửa hàng hiện tại</span>
        <b>{{ storeId }}</b>
        <small>{{ adapter.mode === "api" ? "Dữ liệu API" : "Dữ liệu demo" }}</small>
      </div>
      <button
        v-if="sync"
        class="stocktake-secondary"
        type="button"
        :disabled="!connected || !pendingCount || syncing"
        @click="syncPending"
      >
        {{ syncing ? "Đang đồng bộ…" : `Đồng bộ ${pendingCount} phiếu` }}
      </button>
    </header>

    <div v-if="loading" class="stocktake-state" role="status" aria-live="polite">
      <span class="stocktake-spinner" aria-hidden="true"></span>
      <div>
        <b>Đang tải lô hàng</b>
        <p>Đọc số tồn mới nhất của cửa hàng trước khi nhập số đếm.</p>
      </div>
    </div>

    <div v-else-if="pageError" class="stocktake-state stocktake-state-error" role="alert">
      <div>
        <b>Không tải được màn kiểm kê</b>
        <p>{{ pageError }}</p>
      </div>
      <button class="stocktake-secondary" type="button" @click="loadInventory">Thử lại</button>
    </div>

    <div v-else-if="!inventory.batches.length" class="stocktake-state">
      <div>
        <b>Chưa có lô hàng để kiểm kê</b>
        <p>Cửa hàng này chưa có tồn theo lô. Nhận hàng trước rồi quay lại màn hình này.</p>
      </div>
    </div>

    <div v-else class="stocktake-layout">
      <form class="stocktake-form" novalidate @submit.prevent="submit">
        <div class="stocktake-section-title">
          <div>
            <h2>Ghi số đếm</h2>
            <p>Mỗi phiếu chỉ ghi cho một lô để giữ đúng dấu vết tồn.</p>
          </div>
          <span>{{ inventory.batches.length }} lô</span>
        </div>

        <label for="stocktake-batch">Lô hàng</label>
        <select id="stocktake-batch" v-model="selectedBatchId" :disabled="submitting">
          <option v-for="batch in inventory.batches" :key="batch.batchId" :value="batch.batchId">
            {{ productName(batch.productId) }} · {{ batch.batchNumber }}
          </option>
        </select>

        <div v-if="selectedBatch" class="batch-context">
          <div>
            <span>Sản phẩm</span>
            <b>{{ selectedProduct?.productName || selectedBatch.productId }}</b>
          </div>
          <div>
            <span>Lô</span>
            <b>{{ selectedBatch.batchNumber }}</b>
            <small v-if="selectedBatch.supplierLotNumber">
              Lô NCC {{ selectedBatch.supplierLotNumber }}
            </small>
          </div>
          <div>
            <span>Đơn vị</span>
            <b>{{ unitCode || "Chưa có trong API" }}</b>
          </div>
        </div>

        <dl class="count-summary">
          <div>
            <dt>Tồn hệ thống</dt>
            <dd>
              {{ formatQuantity(selectedBatch?.onHandQuantity || 0) }}
              <small>{{ unitCode || "" }}</small>
            </dd>
          </div>
          <div>
            <dt>Số thực tế</dt>
            <dd>
              {{ validation.value === undefined ? "—" : formatQuantity(validation.value) }}
              <small>{{ unitCode || "" }}</small>
            </dd>
          </div>
          <div>
            <dt>Chênh lệch</dt>
            <dd :class="{ 'has-difference': difference !== undefined && difference !== 0 }">
              {{
                difference === undefined
                  ? "—"
                  : `${difference > 0 ? "+" : ""}${formatQuantity(difference)}`
              }}
              <small>{{ unitCode || "" }}</small>
            </dd>
          </div>
        </dl>

        <label for="stocktake-actual">Số lượng thực tế</label>
        <div class="quantity-field">
          <input
            id="stocktake-actual"
            v-model="actualQuantity"
            :aria-describedby="
              'stocktake-quantity-hint' + (formError ? ' stocktake-form-error' : '')
            "
            :aria-invalid="!!formError"
            :disabled="submitting || !canWrite"
            :inputmode="quantityScale(unitCode) === 3 ? 'decimal' : 'numeric'"
            autocomplete="off"
            placeholder="0"
            type="text"
          />
          <span v-if="unitCode">{{ unitCode }}</span>
        </div>
        <p id="stocktake-quantity-hint" class="field-hint">{{ inputHint }}</p>

        <label for="stocktake-note">Ghi chú <small>(không bắt buộc)</small></label>
        <textarea
          id="stocktake-note"
          v-model="note"
          aria-label="Ghi chú"
          :disabled="submitting || !canWrite"
          maxlength="500"
          placeholder="Vị trí kệ, tình trạng bao bì hoặc lý do chênh lệch…"
        ></textarea>

        <p v-if="!canWrite" class="stocktake-notice" role="alert">
          {{ writeUnavailableReason || "Bạn không có quyền ghi nhận kiểm kê tại cửa hàng này." }}
        </p>
        <p v-if="formError" id="stocktake-form-error" class="stocktake-error" role="alert">
          {{ formError }}
        </p>
        <p v-if="statusMessage" class="stocktake-success" role="status">{{ statusMessage }}</p>

        <button
          class="stocktake-primary"
          type="submit"
          :disabled="submitting || !canWrite || !selectedBatch"
        >
          {{
            submitting ? "Đang lưu số đếm…" : connected ? "Gửi phiếu kiểm kê" : "Lưu phiếu offline"
          }}
        </button>
      </form>

      <section class="stocktake-history" aria-labelledby="stocktake-history-title">
        <div class="stocktake-section-title">
          <div>
            <h2 id="stocktake-history-title">Phiếu gần đây</h2>
            <p>Trạng thái lưu và xử lý của từng lần đếm.</p>
          </div>
          <span>{{ records.length }}</span>
        </div>

        <div v-if="!records.length" class="stocktake-empty">
          <b>Chưa có phiếu kiểm kê</b>
          <p>Phiếu vừa lưu sẽ xuất hiện ở đây cùng trạng thái tiếp theo.</p>
        </div>

        <article v-for="record in records" :key="record.id" class="stocktake-record count-row">
          <div class="record-heading">
            <div>
              <b>{{ productName(record.productId) }}</b>
              <small>{{ record.batchId }} · {{ formatTime(record.countedAt) }}</small>
            </div>
            <span :class="['record-status', `is-${record.status.toLowerCase()}`]">
              {{ statusLabels[record.status] }}
            </span>
          </div>
          <dl>
            <div>
              <dt>Hệ thống</dt>
              <dd>{{ formatQuantity(record.expectedQuantity, record.productId) }}</dd>
            </div>
            <div>
              <dt>Thực tế</dt>
              <dd>{{ formatQuantity(record.actualQuantity, record.productId) }}</dd>
            </div>
            <div>
              <dt>Đơn vị</dt>
              <dd>{{ productUnit(record.productId) }}</dd>
            </div>
          </dl>
          <p v-if="record.note" class="record-note">{{ record.note }}</p>
          <button
            v-if="approve && record.status === 'REVIEW'"
            class="stocktake-secondary"
            type="button"
            :disabled="!connected || !!approvingId"
            @click="approveRecord(record)"
          >
            {{ approvingId === record.id ? "Đang duyệt…" : "Duyệt điều chỉnh tồn" }}
          </button>
          <p v-if="record.status === 'CONFLICT'" class="stocktake-error">
            Tồn lô đã thay đổi sau lúc đếm. Tạo phiếu kiểm lại; hệ thống không ghi đè tự động.
          </p>
        </article>
      </section>
    </div>
  </section>
</template>

<style scoped>
.stocktake-workspace {
  min-width: 0;
  color: #55495a;
}

.stocktake-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(230px, auto) auto;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
}

.stocktake-header h1,
.stocktake-section-title h2 {
  margin: 0;
  color: #3e2948;
  letter-spacing: -0.025em;
}

.stocktake-header h1 {
  font-size: clamp(1.8rem, 3vw, 2.5rem);
}

.stocktake-header p,
.stocktake-section-title p,
.stocktake-state p,
.stocktake-empty p {
  margin: 8px 0 0;
  color: #756b79;
  line-height: 1.65;
}

.stocktake-context {
  display: grid;
  min-width: 0;
  gap: 4px;
  padding: 13px 16px;
  border-radius: 14px;
  background: #f4eef7;
}

.stocktake-context span,
.stocktake-context small,
.batch-context span,
.batch-context small {
  color: #756b79;
  font-size: 0.75rem;
}

.stocktake-context b {
  overflow-wrap: anywhere;
  color: #4f2e5d;
  font-size: 0.875rem;
}

.stocktake-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  align-items: start;
  gap: 22px;
  min-width: 0;
}

.stocktake-form,
.stocktake-history,
.stocktake-state {
  min-width: 0;
  border: 1px solid #ebe4ee;
  border-radius: 16px;
  background: #fff;
}

.stocktake-form,
.stocktake-history {
  padding: clamp(20px, 3vw, 30px);
}

.stocktake-form {
  display: grid;
  gap: 12px;
}

.stocktake-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 12px;
}

.stocktake-section-title h2 {
  font-size: 1.2rem;
}

.stocktake-section-title > span,
.record-status {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #f1ebf4;
  color: #644773;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 7px 10px;
}

.stocktake-form > label {
  margin-top: 6px;
  color: #493b50;
  font-size: 0.875rem;
  font-weight: 700;
}

.stocktake-form > label small {
  font-weight: 500;
}

.stocktake-form select,
.stocktake-form input,
.stocktake-form textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid #dcd3e1;
  border-radius: 12px;
  background: #fff;
  color: #3f3344;
  font: inherit;
  font-size: 1rem;
}

.stocktake-form select,
.stocktake-form input {
  min-height: 48px;
  padding: 0 14px;
}

.stocktake-form textarea {
  min-height: 104px;
  padding: 13px 14px;
  resize: vertical;
}

.stocktake-form select:focus-visible,
.stocktake-form input:focus-visible,
.stocktake-form textarea:focus-visible,
.stocktake-primary:focus-visible,
.stocktake-secondary:focus-visible {
  outline: 3px solid #9f65bb55;
  outline-offset: 2px;
}

.batch-context {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) minmax(80px, 0.55fr);
  gap: 12px;
  margin: 4px 0 8px;
  padding: 15px;
  border-radius: 14px;
  background: #faf8fb;
}

.batch-context > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.batch-context b {
  overflow-wrap: anywhere;
  font-size: 0.875rem;
}

.count-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 4px 0 8px;
  border-radius: 14px;
  background: #4f355d;
  color: #fff;
}

.count-summary > div {
  min-width: 0;
  padding: 18px;
}

.count-summary > div + div {
  border-left: 1px solid #ffffff24;
}

.count-summary dt {
  color: #e8ddeb;
  font-size: 0.75rem;
}

.count-summary dd {
  margin: 8px 0 0;
  overflow-wrap: anywhere;
  font-size: clamp(1.3rem, 3vw, 1.75rem);
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.count-summary dd small {
  font-size: 0.7rem;
  font-weight: 600;
}

.count-summary .has-difference {
  color: #ffd38c;
}

.quantity-field {
  position: relative;
}

.quantity-field input {
  padding-right: 62px;
  font-size: 1.5rem;
  font-variant-numeric: tabular-nums;
  font-weight: 750;
}

.quantity-field > span {
  position: absolute;
  top: 50%;
  right: 14px;
  transform: translateY(-50%);
  color: #756b79;
  font-size: 0.8rem;
  font-weight: 700;
}

.field-hint,
.stocktake-error,
.stocktake-success,
.stocktake-notice,
.record-note {
  margin: 0;
  line-height: 1.55;
  overflow-wrap: anywhere;
  font-size: 0.8rem;
}

.field-hint {
  color: #756b79;
}

.stocktake-error {
  color: #9f2437;
}

.stocktake-success {
  color: #17663b;
}

.stocktake-notice {
  padding: 13px 14px;
  border: 1px solid #e4d6a5;
  border-radius: 12px;
  background: #fff9e9;
  color: #665425;
}

.stocktake-primary,
.stocktake-secondary {
  min-height: 44px;
  border-radius: 12px;
  font: inherit;
  font-size: 0.875rem;
  font-weight: 750;
  cursor: pointer;
}

.stocktake-primary {
  margin-top: 4px;
  border: 0;
  background: #5b3769;
  color: #fff;
  padding: 0 20px;
}

.stocktake-secondary {
  border: 1px solid #d8cade;
  background: #fff;
  color: #583866;
  padding: 0 14px;
}

.stocktake-primary:disabled,
.stocktake-secondary:disabled,
.stocktake-form :disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.stocktake-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 220px;
  padding: 28px;
  text-align: left;
}

.stocktake-state-error {
  justify-content: space-between;
  border-color: #efcfd5;
  background: #fffafb;
}

.stocktake-spinner {
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border: 3px solid #e4d9e8;
  border-top-color: #6e437c;
  border-radius: 50%;
  animation: stocktake-spin 0.8s linear infinite;
}

.stocktake-empty {
  padding: 28px 12px;
  text-align: center;
}

.stocktake-record {
  padding: 20px 0;
  border-top: 1px solid #eee8f0;
}

.record-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.record-heading > div {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.record-heading b,
.record-heading small {
  overflow-wrap: anywhere;
}

.record-heading small {
  color: #756b79;
  font-size: 0.75rem;
}

.record-status.is-approved {
  background: #e6f4ec;
  color: #17663b;
}

.record-status.is-conflict {
  background: #fce9ed;
  color: #9f2437;
}

.record-status.is-pending,
.record-status.is-review {
  background: #fff3d8;
  color: #705a1c;
}

.stocktake-record dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 14px 0;
}

.stocktake-record dl > div {
  min-width: 0;
  padding: 10px;
  border-radius: 10px;
  background: #f8f5f9;
}

.stocktake-record dt {
  color: #756b79;
  font-size: 0.7rem;
}

.stocktake-record dd {
  margin: 5px 0 0;
  overflow-wrap: anywhere;
  font-variant-numeric: tabular-nums;
  font-weight: 750;
}

.record-note {
  margin: 0 0 14px;
  color: #655a69;
}

@keyframes stocktake-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1050px) {
  .stocktake-header {
    grid-template-columns: minmax(0, 1fr) minmax(220px, auto);
  }

  .stocktake-header > button {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .stocktake-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stocktake-header {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .stocktake-header > button,
  .stocktake-primary,
  .stocktake-state .stocktake-secondary {
    width: 100%;
  }

  .stocktake-form,
  .stocktake-history {
    padding: 18px;
  }

  .batch-context {
    grid-template-columns: minmax(0, 1fr) minmax(95px, 0.65fr);
  }

  .batch-context > div:first-child {
    grid-column: 1 / -1;
  }

  .stocktake-state,
  .stocktake-state-error {
    align-items: stretch;
    flex-direction: column;
    min-height: 0;
  }
}

@media (max-width: 430px) {
  .count-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .count-summary > div {
    padding: 15px;
  }

  .count-summary > div:nth-child(3) {
    grid-column: 1 / -1;
    border-top: 1px solid #ffffff24;
    border-left: 0;
  }

  .stocktake-section-title,
  .record-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .stocktake-section-title > span,
  .record-status {
    align-self: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stocktake-spinner {
    animation: none;
  }
}
</style>
