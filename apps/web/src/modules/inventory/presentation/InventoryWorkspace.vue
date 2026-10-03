<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  InventoryApiError,
  type CatalogProduct,
  type CatalogSupplier,
  type InventoryPort,
  type InventorySnapshot,
  type PendingReceiptOperation,
  type Receipt,
  type ReceiptDraft,
} from "../domain/inventory";
import { validateReceipt, vietnamBusinessDate, type ReceiptErrors } from "../domain/validation";
import {
  clearPendingOperation,
  loadPendingOperation,
  savePendingOperation,
} from "../infrastructure/operation-session";

const props = defineProps<{
  view: "receive" | "inventory";
  adapter: InventoryPort;
  userId: string;
  online: boolean;
  canReceive: boolean;
}>();

const suppliers = ref<CatalogSupplier[]>([]);
const products = ref<CatalogProduct[]>([]);
const receipts = ref<Receipt[]>([]);
const inventory = ref<InventorySnapshot>({ products: [], batches: [], movements: [] });
const loading = ref(false);
const submitting = ref(false);
const pageError = ref("");
const success = ref("");
const fieldErrors = ref<ReceiptErrors>({});
const pendingOperation = ref<PendingReceiptOperation>();
const recoveryChecking = ref(false);

const draft = reactive<ReceiptDraft>({
  supplierId: "",
  productId: "",
  expectedQuantity: "10",
  deliveredQuantity: "10",
  acceptedQuantity: "10",
  rejectedQuantity: "0",
  unitCost: "0",
  supplierLotNumber: "",
  expiryDate: "",
  discrepancyReason: "",
});

const selectedProduct = computed(() =>
  products.value.find((product) => product.id === draft.productId),
);
const productNames = computed(() =>
  Object.fromEntries([
    ...products.value.map((product) => [product.id, product.name]),
    ...inventory.value.products.map((product) => [product.productId, product.productName]),
  ]),
);
const supplierNames = computed(() =>
  Object.fromEntries(suppliers.value.map((supplier) => [supplier.id, supplier.name])),
);

onMounted(async () => {
  await loadCurrentView();
  if (props.adapter.mode === "api" && props.canReceive) await reconcilePendingOperation();
});

watch(
  () => props.view,
  () => void loadCurrentView(),
);

async function loadCurrentView() {
  loading.value = true;
  pageError.value = "";
  try {
    if (props.view === "receive") {
      if (!props.canReceive) return;
      const [catalog, recent] = await Promise.all([
        props.adapter.loadCatalog(),
        props.adapter.listReceipts(),
      ]);
      suppliers.value = catalog.suppliers;
      products.value = catalog.products;
      receipts.value = recent;
      if (!draft.supplierId) draft.supplierId = suppliers.value[0]?.id || "";
      if (!draft.productId) draft.productId = products.value[0]?.id || "";
    } else {
      inventory.value = await props.adapter.loadInventory();
    }
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
}

async function submitReceipt() {
  if (submitting.value || !props.canReceive) return;
  pageError.value = "";
  success.value = "";
  fieldErrors.value = validateReceipt(draft, selectedProduct.value);
  if (Object.keys(fieldErrors.value).length) {
    pageError.value = "Kiểm tra lại các ô được đánh dấu.";
    return;
  }
  if (!props.online) {
    pageError.value = "Nhận hàng không được xếp hàng offline. Hãy kết nối mạng rồi thử lại.";
    return;
  }
  const operation: PendingReceiptOperation = {
    idempotencyKey: crypto.randomUUID(),
    clientOperationId: crypto.randomUUID(),
    draft: { ...draft },
    state: "posting",
  };
  await postOperation(operation);
}

async function postOperation(operation: PendingReceiptOperation) {
  submitting.value = true;
  pageError.value = "";
  fieldErrors.value = {};
  pendingOperation.value = operation;
  if (props.adapter.mode === "api") savePendingOperation(props.userId, operation);
  try {
    await props.adapter.confirmReceipt(operation);
    clearOperation();
    success.value =
      props.adapter.mode === "api"
        ? "Đã xác nhận phiếu. Tồn kho vừa được đọc lại từ máy chủ."
        : "Đã xác nhận phiếu và cập nhật tồn kho demo.";
    draft.supplierLotNumber = "";
    draft.discrepancyReason = "";
  } catch (error) {
    const apiError = error instanceof InventoryApiError ? error : undefined;
    if (
      props.adapter.mode === "api" &&
      (!apiError || apiError.status === undefined || apiError.status >= 500)
    ) {
      const uncertain = { ...operation, state: "uncertain" as const };
      pendingOperation.value = uncertain;
      savePendingOperation(props.userId, uncertain);
      pageError.value =
        apiError?.message ||
        "Chưa biết máy chủ đã nhận phiếu hay chưa. Hãy kiểm tra trạng thái trước khi thử lại.";
    } else {
      clearOperation();
      showError(error);
    }
    submitting.value = false;
    return;
  }
  try {
    const [recent, snapshot] = await Promise.all([
      props.adapter.listReceipts(),
      props.adapter.loadInventory(),
    ]);
    receipts.value = recent;
    inventory.value = snapshot;
  } catch (error) {
    success.value = "Đã xác nhận phiếu, nhưng chưa đọc lại được tồn kho.";
    showError(error);
  } finally {
    submitting.value = false;
  }
}

async function reconcilePendingOperation() {
  const operation = loadPendingOperation(props.userId);
  if (!operation) return;
  pendingOperation.value = operation;
  recoveryChecking.value = true;
  pageError.value = "";
  try {
    const receipt = await props.adapter.findReceiptByClientOperationId(operation.clientOperationId);
    if (receipt) {
      clearOperation();
      success.value = `Máy chủ đã ghi phiếu ${receipt.id}. Không gửi lại thao tác.`;
      const [recent, snapshot] = await Promise.all([
        props.adapter.listReceipts(),
        props.adapter.loadInventory(),
      ]);
      receipts.value = recent;
      inventory.value = snapshot;
    } else {
      pageError.value =
        "Chưa tìm thấy phiếu cho thao tác trước. Hệ thống không tự gửi lại; bạn có thể kiểm tra lại hoặc chủ động thử lại cùng mã.";
    }
  } catch (error) {
    showError(error);
  } finally {
    recoveryChecking.value = false;
  }
}

function retryPendingOperation() {
  if (pendingOperation.value && !submitting.value) void postOperation(pendingOperation.value);
}

function discardPendingOperation() {
  clearOperation();
  pageError.value = "Đã bỏ dấu thao tác cũ. Lần xác nhận tiếp theo sẽ là một thao tác mới.";
}

function clearOperation() {
  pendingOperation.value = undefined;
  if (props.adapter.mode === "api") clearPendingOperation(props.userId);
}

function showError(error: unknown) {
  const apiError = error instanceof InventoryApiError ? error : undefined;
  pageError.value = error instanceof Error ? error.message : "Không thể tải dữ liệu.";
  if (apiError) fieldErrors.value = apiError.fieldErrors as ReceiptErrors;
}

function fieldError(field: keyof ReceiptDraft) {
  return fieldErrors.value[field];
}

function expiryLabel(status: string) {
  return (
    {
      EXPIRED: "Hết hạn",
      NEAR_EXPIRY: "Cận hạn",
      VALID: "Còn hạn",
      NO_EXPIRY: "Không theo hạn",
    }[status] || status
  );
}

function movementLabel(type: string) {
  return { RECEIPT: "Nhận hàng", SALE: "Bán hàng", ADJUSTMENT: "Điều chỉnh" }[type] || type;
}

function sourceLabel(type: string) {
  return (
    { GOODS_RECEIPT: "Phiếu nhận", INVOICE: "Hóa đơn", ADJUSTMENT: "Phiếu điều chỉnh" }[type] ||
    type
  );
}

function dateTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function money(value: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
}
</script>

<template>
  <section v-if="view === 'receive'" class="inventory-feature">
    <div class="page-head">
      <div>
        <h1>Nhận hàng & nhập kho</h1>
        <p>Đối chiếu số giao, số nhận và hàng từ chối trước khi tăng tồn.</p>
      </div>
      <span class="business-date">Ngày nghiệp vụ {{ vietnamBusinessDate() }}</span>
    </div>

    <div v-if="pendingOperation" class="operation-notice" role="status">
      <div>
        <strong>Thao tác trước chưa có kết luận</strong>
        <p>
          Mã đối soát {{ pendingOperation.clientOperationId }}. Không có phiếu nào được tự động gửi
          lại.
        </p>
      </div>
      <div class="operation-actions">
        <button
          class="secondary"
          :disabled="recoveryChecking || submitting"
          @click="reconcilePendingOperation"
        >
          {{ recoveryChecking ? "Đang kiểm tra…" : "Kiểm tra máy chủ" }}
        </button>
        <button class="primary" :disabled="submitting || !online" @click="retryPendingOperation">
          {{ submitting ? "Đang gửi…" : "Thử lại cùng mã" }}
        </button>
        <button class="text-button" :disabled="submitting" @click="discardPendingOperation">
          Bỏ dấu thao tác
        </button>
      </div>
    </div>

    <p v-if="pageError" class="feature-message error" role="alert">{{ pageError }}</p>
    <p v-if="success" class="feature-message success-message" role="status">{{ success }}</p>

    <div v-if="!canReceive" class="card empty">
      Bạn không có quyền đọc hoặc xác nhận phiếu nhận.
    </div>
    <div v-else-if="loading" class="card empty">
      {{
        adapter.mode === "api"
          ? "Đang tải danh mục và phiếu nhận từ máy chủ…"
          : "Đang mở dữ liệu demo…"
      }}
    </div>
    <div v-else class="receipt-layout">
      <form class="card receipt-form" novalidate @submit.prevent="submitReceipt">
        <div class="form-title">
          <div>
            <h2>Phiếu nhận hàng mới</h2>
            <p>Một sản phẩm mỗi lần xác nhận để kiểm soát lô rõ ràng.</p>
          </div>
          <span class="status">{{
            adapter.mode === "api" ? "Dữ liệu máy chủ" : "Dữ liệu demo"
          }}</span>
        </div>

        <div class="form-row">
          <label>
            Nhà cung cấp
            <select v-model="draft.supplierId" :aria-invalid="!!fieldError('supplierId')">
              <option value="" disabled>Chọn nhà cung cấp</option>
              <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
                {{ supplier.code }} · {{ supplier.name }}
              </option>
            </select>
            <small v-if="fieldError('supplierId')" class="field-error">{{
              fieldError("supplierId")
            }}</small>
          </label>
          <label>
            Sản phẩm
            <select v-model="draft.productId" :aria-invalid="!!fieldError('productId')">
              <option value="" disabled>Chọn sản phẩm</option>
              <option v-for="product in products" :key="product.id" :value="product.id">
                {{ product.sku }} · {{ product.name }}
              </option>
            </select>
            <small v-if="fieldError('productId')" class="field-error">{{
              fieldError("productId")
            }}</small>
          </label>
        </div>

        <div class="quantity-grid">
          <label
            v-for="item in [
              ['expectedQuantity', 'Số dự kiến'],
              ['deliveredQuantity', 'Số giao'],
              ['acceptedQuantity', 'Số nhận'],
              ['rejectedQuantity', 'Số từ chối'],
            ]"
            :key="item[0]"
          >
            {{ item[1] }}
            <input
              v-model="draft[item[0] as keyof ReceiptDraft]"
              inputmode="decimal"
              :aria-invalid="!!fieldError(item[0] as keyof ReceiptDraft)"
              placeholder="0.000"
            />
            <small v-if="fieldError(item[0] as keyof ReceiptDraft)" class="field-error">
              {{ fieldError(item[0] as keyof ReceiptDraft) }}
            </small>
          </label>
        </div>

        <div class="form-row">
          <label>
            Giá nhập (VND)
            <input
              v-model="draft.unitCost"
              inputmode="numeric"
              :aria-invalid="!!fieldError('unitCost')"
            />
            <small v-if="fieldError('unitCost')" class="field-error">{{
              fieldError("unitCost")
            }}</small>
          </label>
          <label>
            Số lô nhà cung cấp
            <input v-model.trim="draft.supplierLotNumber" placeholder="Ví dụ: SUA-2026-10" />
          </label>
        </div>

        <div class="form-row">
          <label>
            Hạn sử dụng {{ selectedProduct?.tracksExpiry ? "(bắt buộc)" : "(nếu có)" }}
            <input
              v-model="draft.expiryDate"
              type="date"
              :min="vietnamBusinessDate()"
              :aria-invalid="!!fieldError('expiryDate')"
            />
            <small v-if="fieldError('expiryDate')" class="field-error">{{
              fieldError("expiryDate")
            }}</small>
          </label>
          <label>
            Lý do sai lệch / từ chối
            <textarea
              v-model.trim="draft.discrepancyReason"
              :aria-invalid="!!fieldError('discrepancyReason')"
              placeholder="Ví dụ: 2 hộp móp bao bì"
            ></textarea>
            <small v-if="fieldError('discrepancyReason')" class="field-error">{{
              fieldError("discrepancyReason")
            }}</small>
          </label>
        </div>

        <button class="primary wide" :disabled="submitting || !online || !!pendingOperation">
          {{
            submitting
              ? "Đang xác nhận…"
              : online
                ? "Xác nhận phiếu nhận"
                : "Cần kết nối để xác nhận"
          }}
        </button>
      </form>

      <section class="card recent-receipts">
        <div class="card-head">
          <div>
            <h2>Phiếu gần đây</h2>
            <p>
              {{
                adapter.mode === "api"
                  ? "Đọc trực tiếp trong phạm vi cửa hàng của phiên."
                  : "Lưu trong dữ liệu demo trên thiết bị."
              }}
            </p>
          </div>
          <button class="secondary compact" :disabled="loading" @click="loadCurrentView">
            Làm mới
          </button>
        </div>
        <div v-if="!receipts.length" class="empty">Chưa có phiếu nhận hàng.</div>
        <article v-for="receipt in receipts" :key="receipt.id" class="receipt-row">
          <div class="receipt-row-title">
            <div>
              <b>{{ receipt.id }}</b>
              <small
                >{{ supplierNames[receipt.supplierId] || receipt.supplierId }} ·
                {{ receipt.lines.length }} dòng · {{ dateTime(receipt.receivedAt) }}</small
              >
            </div>
            <span class="status">{{ receipt.status }}</span>
          </div>
          <div v-for="line in receipt.lines" :key="line.id" class="receipt-item-line">
            <p>
              <b>{{ productNames[line.productId] || line.productId }}</b> · Nhận
              <strong>{{ line.acceptedQuantity }}</strong> · Từ chối
              {{ line.rejectedQuantity }}
            </p>
            <small
              >Lô {{ line.supplierLotNumber || "—" }} · Giá nhập {{ money(line.unitCost) }}</small
            >
          </div>
        </article>
      </section>
    </div>
  </section>

  <section v-else class="inventory-feature">
    <div class="page-head">
      <div>
        <h1>Tồn kho, lô & biến động</h1>
        <p>On-hand gồm mọi lô; available chỉ gồm lượng có thể bán tại ngày Việt Nam.</p>
      </div>
      <button class="secondary" :disabled="loading" @click="loadCurrentView">
        {{ loading ? "Đang đọc…" : "Làm mới dữ liệu" }}
      </button>
    </div>
    <p v-if="pageError" class="feature-message error" role="alert">{{ pageError }}</p>
    <div v-if="loading" class="card empty">
      {{ adapter.mode === "api" ? "Đang đọc tồn kho từ máy chủ…" : "Đang mở tồn kho demo…" }}
    </div>
    <template v-else>
      <section class="inventory-summary" aria-label="Tổng tồn theo sản phẩm">
        <article
          v-for="product in inventory.products"
          :key="product.productId"
          class="card stock-summary"
        >
          <div>
            <span>{{ product.sku }}</span
            ><span class="status">{{ product.productStatus }}</span>
          </div>
          <h2>{{ product.productName }}</h2>
          <dl>
            <div>
              <dt>On-hand</dt>
              <dd>{{ product.onHandQuantity }}</dd>
            </div>
            <div>
              <dt>Available</dt>
              <dd>{{ product.availableQuantity }}</dd>
            </div>
          </dl>
        </article>
        <div v-if="!inventory.products.length" class="card empty">
          Chưa có tồn sản phẩm trong cửa hàng này.
        </div>
      </section>

      <section class="card inventory-section">
        <div class="card-head">
          <div>
            <h2>Lô và hạn sử dụng</h2>
            <p>
              {{
                adapter.mode === "api"
                  ? "Trạng thái hạn do server tính theo Asia/Ho_Chi_Minh."
                  : "Trạng thái hạn được mô phỏng theo ngày Việt Nam."
              }}
            </p>
          </div>
          <span>{{ inventory.batches.length }} lô</span>
        </div>
        <div class="inventory-table-wrap">
          <table class="inventory-table">
            <thead>
              <tr>
                <th>Sản phẩm / lô</th>
                <th>Hạn</th>
                <th>On-hand</th>
                <th>Available</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in inventory.batches" :key="batch.batchId">
                <td data-label="Sản phẩm / lô">
                  <b>{{ productNames[batch.productId] || batch.productId }}</b
                  ><small
                    >{{ batch.batchNumber
                    }}<template v-if="batch.supplierLotNumber">
                      · NCC {{ batch.supplierLotNumber }}</template
                    ></small
                  >
                </td>
                <td data-label="Hạn">{{ batch.expiryDate || "Không có" }}</td>
                <td data-label="On-hand">{{ batch.onHandQuantity }}</td>
                <td data-label="Available">{{ batch.availableQuantity }}</td>
                <td data-label="Trạng thái">
                  <span
                    :class="[
                      'status',
                      {
                        danger: batch.expiryStatus === 'EXPIRED',
                        amber: batch.expiryStatus === 'NEAR_EXPIRY',
                      },
                    ]"
                    >{{ expiryLabel(batch.expiryStatus) }}</span
                  ><small>{{ batch.status }}</small>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="card inventory-section movement-section">
        <div class="card-head">
          <div>
            <h2>Biến động tồn</h2>
            <p>Mỗi dòng chỉ rõ loại và chứng từ nguồn.</p>
          </div>
          <span>{{ inventory.movements.length }} thao tác</span>
        </div>
        <div v-if="!inventory.movements.length" class="empty">Chưa có biến động tồn.</div>
        <article
          v-for="movement in inventory.movements"
          :key="movement.movementId"
          class="movement-entry"
        >
          <div>
            <b>{{ productNames[movement.productId] || movement.productId }}</b
            ><small
              >{{ movementLabel(movement.type) }} · {{ sourceLabel(movement.source.type) }}
              {{ movement.source.id }} · {{ dateTime(movement.occurredAt) }}</small
            >
          </div>
          <strong :class="Number(movement.quantityDelta) >= 0 ? 'positive' : 'negative'"
            >{{ Number(movement.quantityDelta) > 0 ? "+" : "" }}{{ movement.quantityDelta }}</strong
          >
        </article>
      </section>
    </template>
  </section>
</template>
