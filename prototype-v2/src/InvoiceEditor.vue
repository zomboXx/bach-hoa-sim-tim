<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { available, price, type State, type Product } from "./api";

const props = defineProps<{
  state: State;
  cart: { id: string; quantity: number }[];
  connected: boolean;
  busy: boolean;
  total: number;
}>();
const emit = defineEmits<{
  add: [id: string];
  quantity: [id: string, value: number];
  pay: [];
}>();
const method = defineModel<string>("method", { required: true });
const cash = defineModel<number>("cash", { required: true });
const search = ref("");
const searchInput = ref<HTMLInputElement>();
const cameraDialog = ref<HTMLDialogElement>();
const message = ref("");
const money = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    n,
  );
const product = (id: string) => props.state.products.find((p) => p.id === id)!;
const unitPrice = (id: string) => price(props.state, product(id));
const matches = computed(() => {
  const term = search.value.trim().toLocaleLowerCase("vi");
  return term
    ? props.state.products.filter(
        (p) =>
          p.active !== false &&
          `${p.name} ${p.barcode} ${p.id}`
            .toLocaleLowerCase("vi")
            .includes(term),
      )
    : [];
});
const units = computed(() =>
  props.cart.reduce((sum, row) => sum + row.quantity, 0),
);
function choose(p: Product) {
  if (props.busy || !props.connected) return;
  emit("add", p.id);
  search.value = "";
  message.value = "";
  searchInput.value?.focus();
}
function submitSearch() {
  if (!search.value.trim()) return;
  const term = search.value.trim().toLocaleLowerCase("vi");
  const exact = matches.value.find(
    (p) => p.barcode === term || p.id.toLocaleLowerCase("vi") === term,
  );
  const p =
    exact || (matches.value.length === 1 ? matches.value[0] : undefined);
  if (p) choose(p);
  else
    message.value = matches.value.length
      ? "Có nhiều sản phẩm phù hợp. Chọn một sản phẩm bên dưới."
      : "Không tìm thấy sản phẩm. Kiểm tra lại tên hoặc mã vạch.";
}
function digitsOnly(event: Event) {
  const input = event as InputEvent;
  if (input.data && /[^0-9]/.test(input.data)) input.preventDefault();
}
async function editQuantity(id: string, event: Event) {
  const input = event.target as HTMLInputElement;
  const previous = props.cart.find((row) => row.id === id)?.quantity;
  // Empty is a temporary editing state only; it never changes invoice quantities.
  if (input.value === "") return;
  if (
    !/^[0-9]+$/.test(input.value) ||
    !Number.isSafeInteger(Number(input.value))
  ) {
    input.value = String(previous ?? "");
    return;
  }
  emit("quantity", id, Number(input.value));
  await nextTick();
  input.value = String(props.cart.find((row) => row.id === id)?.quantity ?? "");
}
function restoreQuantity(id: string, event: Event) {
  (event.target as HTMLInputElement).value = String(
    props.cart.find((row) => row.id === id)?.quantity ?? "",
  );
}
</script>

<template>
  <div class="invoice-editor">
    <div class="page-head">
      <div>
        <p class="eyebrow">QUẦY THU NGÂN · HÓA ĐƠN MỚI</p>
        <h1>Hóa đơn đang lập</h1>
        <p>Tra sản phẩm, thêm vào hóa đơn và xác nhận thanh toán.</p>
      </div>
      <button
        class="secondary camera-trigger"
        @click="cameraDialog?.showModal()"
      >
        <span aria-hidden="true">▣</span> Quét mã bằng camera
        <small>Sắp có</small>
      </button>
    </div>
    <div class="invoice-layout">
      <section class="card invoice-sheet" aria-label="Hóa đơn đang lập">
        <div class="invoice-search">
          <label for="invoice-search">Thêm sản phẩm vào hóa đơn</label>
          <form class="search-bar" @submit.prevent="submitSearch">
            <span aria-hidden="true">⌕</span>
            <input
              id="invoice-search"
              ref="searchInput"
              v-model="search"
              :disabled="busy || !connected"
              autocomplete="off"
              placeholder="Tên sản phẩm hoặc mã vạch…"
              aria-label="Tìm sản phẩm bán hàng"
              aria-controls="invoice-search-results"
              @input="message = ''"
              @keydown.esc="
                search = '';
                message = '';
              "
            />
            <button
              type="submit"
              :disabled="busy || !connected || !search.trim()"
            >
              Thêm ↵
            </button>
          </form>
          <p class="search-help">
            Nhập mã vạch rồi nhấn Enter, hoặc tra tên và chọn sản phẩm. Mỗi lượt
            thêm tăng 1.
          </p>
          <p v-if="message" class="search-message" role="status">
            {{ message }}
          </p>
          <div
            v-if="search.trim()"
            id="invoice-search-results"
            class="invoice-results"
            aria-label="Kết quả tìm sản phẩm"
          >
            <button
              v-for="p in matches.slice(0, 8)"
              :key="p.id"
              type="button"
              :aria-label="'Thêm ' + p.name"
              :disabled="busy || !connected || available(state, p.id) === 0"
              @click="choose(p)"
            >
              <span class="result-icon" aria-hidden="true">{{ p.emoji }}</span
              ><span class="result-detail"
                ><b>{{ p.name }}</b
                ><small
                  >{{ p.barcode }} · Còn {{ available(state, p.id) }}
                  {{ p.unit }}</small
                ></span
              ><span class="result-price"
                >{{ money(price(state, p)) }} <b aria-hidden="true">＋</b></span
              >
            </button>
            <p v-if="!matches.length">Không tìm thấy sản phẩm phù hợp.</p>
            <p v-else-if="matches.length > 8">
              Hiển thị 8 / {{ matches.length }} kết quả. Nhập cụ thể hơn để tìm
              nhanh.
            </p>
          </div>
        </div>
        <div class="invoice-sheet-heading">
          <h2>Chi tiết hóa đơn</h2>
          <span>{{ cart.length }} mặt hàng · {{ units }} sản phẩm</span>
        </div>
        <div v-if="!cart.length" class="invoice-empty">
          <span aria-hidden="true">▤</span>
          <h3>Hóa đơn chưa có sản phẩm</h3>
          <p>
            Tra tên hoặc nhập mã vạch ở phía trên<br />để thêm sản phẩm đầu
            tiên.
          </p>
        </div>
        <template v-else>
          <div class="invoice-columns" aria-hidden="true">
            <span>Sản phẩm / Đơn giá</span><span>Số lượng</span
            ><span>Thành tiền</span><span></span>
          </div>
          <div v-for="row in cart" :key="row.id" class="cart-row invoice-line">
            <div class="line-product">
              <b>{{ product(row.id).name }}</b
              ><small>{{ product(row.id).barcode }}</small
              ><span
                >{{ money(unitPrice(row.id)) }} /
                {{ product(row.id).unit }}</span
              >
            </div>
            <div class="stepper">
              <button
                :disabled="busy || !connected"
                :aria-label="'Giảm ' + product(row.id).name"
                @click="emit('quantity', row.id, row.quantity - 1)"
              >
                −
              </button>
              <input
                type="text"
                inputmode="numeric"
                pattern="[0-9]*"
                autocomplete="off"
                :disabled="busy || !connected"
                :aria-label="'Số lượng ' + product(row.id).name"
                :value="row.quantity"
                @beforeinput="digitsOnly"
                @input="editQuantity(row.id, $event)"
                @blur="restoreQuantity(row.id, $event)"
                @keydown.enter.prevent="restoreQuantity(row.id, $event)"
              />
              <button
                :disabled="busy || !connected"
                :aria-label="'Tăng ' + product(row.id).name"
                @click="emit('quantity', row.id, row.quantity + 1)"
              >
                +
              </button>
            </div>
            <strong class="line-total">{{
              money(unitPrice(row.id) * row.quantity)
            }}</strong>
            <button
              class="remove-line"
              :disabled="busy || !connected"
              :aria-label="'Xóa ' + product(row.id).name + ' khỏi hóa đơn'"
              @click="emit('quantity', row.id, 0)"
            >
              ×
            </button>
          </div>
        </template>
        <p class="invoice-footnote">
          Số lượng bằng 0 sẽ xóa sản phẩm khỏi hóa đơn. Hóa đơn chỉ được ghi
          nhận sau khi thanh toán.
        </p>
      </section>
      <aside class="card invoice-payment" aria-label="Thanh toán hóa đơn">
        <div class="card-head">
          <h2>Thanh toán</h2>
          <span>{{ units }} sản phẩm</span>
        </div>
        <div class="cart-bottom">
          <div class="total">
            <span>Tổng thanh toán</span><b>{{ money(total) }}</b>
          </div>
          <label
            >Phương thức<select v-model="method" :disabled="busy">
              <option>Tiền mặt</option>
              <option>Chuyển khoản (mô phỏng)</option>
            </select></label
          >
          <label v-if="method === 'Tiền mặt'"
            >Khách đưa<input
              v-model.number="cash"
              :disabled="busy"
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
            @click="emit('pay')"
          >
            {{ busy ? "Đang xử lý…" : "Xác nhận thanh toán →" }}
          </button>
          <p class="payment-note">
            {{
              connected
                ? "Giá, khuyến mãi và tồn còn hạn được kiểm tra lại tại máy chủ."
                : "Cần kết nối máy chủ để bán hàng."
            }}
          </p>
        </div>
      </aside>
    </div>
    <dialog
      ref="cameraDialog"
      class="camera-info"
      aria-labelledby="camera-title"
      @click="$event.target === cameraDialog && cameraDialog?.close()"
    >
      <span class="pill">TÍNH NĂNG SẮP CÓ</span>
      <h2 id="camera-title">Quét mã bằng camera</h2>
      <p>
        Chức năng này chưa được cập nhật. Hiện tại, hãy nhập mã vạch hoặc tra
        tên sản phẩm.
      </p>
      <p>
        Dự kiến: điện thoại đăng nhập cùng tài khoản nhân viên và kết nối với
        hóa đơn đang lập; mỗi lần quét sẽ thêm sản phẩm vào hóa đơn đó.
      </p>
      <p class="payment-note">
        Bản này chưa mở camera và chưa đồng bộ hóa đơn giữa thiết bị.
      </p>
      <button class="primary wide" @click="cameraDialog?.close()">
        Đã hiểu
      </button>
    </dialog>
  </div>
</template>

<style scoped>
.invoice-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 22px;
  align-items: start;
}
.invoice-layout > * {
  min-width: 0;
}
.invoice-sheet {
  overflow: hidden;
}
.invoice-search {
  padding: 24px;
  border-bottom: 1px solid var(--line);
}
.invoice-search > label {
  font-weight: 600;
  font-size: 13px;
  display: block;
  margin-bottom: 12px;
}
.invoice-search .search-bar {
  margin: 0;
  gap: 10px;
}
.search-bar input {
  min-width: 0;
  font-size: 14px;
}
.search-bar button {
  white-space: nowrap;
  border: 0;
  background: #efe7f5;
  color: #643e7c;
  border-radius: 7px;
  padding: 10px 12px;
  font-weight: 600;
}
.search-help,
.search-message {
  font-size: 12px;
  line-height: 1.6;
  margin: 10px 0 0;
  color: #82728e;
}
.search-message {
  color: #70428b;
}
.invoice-results {
  margin-top: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
}
.invoice-results > button {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
  padding: 13px;
  text-align: left;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.invoice-results > button:hover:not(:disabled) {
  background: #f6f1fa;
}
.result-icon {
  font-size: 25px;
}
.result-detail {
  display: grid;
  gap: 5px;
  min-width: 0;
  flex: 1;
}
.result-detail b {
  font-size: 13px;
}
.result-detail small {
  font-size: 11px;
  color: #8c7b98;
  overflow-wrap: anywhere;
}
.result-price {
  font-size: 12px;
  color: #6e4786;
  text-align: right;
}
.result-price b {
  margin-left: 8px;
}
.invoice-results > p {
  font-size: 12px;
  margin: 15px;
}
.invoice-sheet-heading {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px;
}
.invoice-sheet-heading h2 {
  margin: 0;
  font-size: 17px;
}
.invoice-sheet-heading > span {
  font-size: 11px;
  color: #897796;
}
.invoice-empty {
  text-align: center;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 32px 16px;
}
.invoice-empty > span {
  font-size: 44px;
  color: #ae96bf;
}
.invoice-empty h3 {
  font-size: 17px;
  margin: 18px 0 8px;
}
.invoice-empty p {
  font-size: 13px;
  line-height: 1.9;
  color: #93849d;
}
.invoice-columns,
.invoice-line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px 110px 36px;
  gap: 14px;
  padding: 16px 24px;
  align-items: center;
}
.invoice-columns {
  font-size: 11px;
  color: #917c9f;
  background: #faf7fc;
}
.invoice-columns > span:nth-child(3) {
  text-align: right;
}
.invoice-line .line-product {
  min-width: 0;
  gap: 6px;
  overflow-wrap: anywhere;
}
.line-product b {
  font-size: 14px;
}
.line-product small {
  font-size: 11px;
  color: #95869f;
}
.line-product > span {
  font-size: 12px;
  color: #765889;
}
.invoice-line .stepper {
  gap: 4px;
}
.invoice-line .stepper button {
  width: 36px;
  height: 38px;
  flex-shrink: 0;
  font-size: 18px;
}
.stepper input {
  width: 52px;
  min-width: 0;
  height: 38px;
  padding: 4px;
  text-align: center;
  font-size: 15px;
  border: 1px solid #ded0e7;
  border-radius: 5px;
}
.line-total {
  text-align: right;
  font-size: 14px;
  color: #643e7c;
}
.remove-line {
  width: 36px;
  height: 38px;
  border: 1px solid #eadde5;
  background: #fff;
  color: #a35b72;
  border-radius: 6px;
  font-size: 22px;
}
.invoice-footnote {
  margin: 0;
  padding: 18px 24px;
  border-top: 1px dashed var(--line);
  font-size: 11px;
  line-height: 1.7;
  color: #94859e;
}
.invoice-payment {
  position: sticky;
  top: 100px;
  overflow: hidden;
}
.invoice-payment .total {
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.payment-note {
  font-size: 11px;
  line-height: 1.8;
  color: #91819d;
}
.camera-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
}
.camera-trigger small {
  background: #ede4f5;
  padding: 4px 7px;
  border-radius: 4px;
  font-size: 10px;
}
.camera-info {
  max-width: 480px;
  width: calc(100% - 32px);
  padding: 28px;
}
.camera-info p {
  font-size: 14px;
  line-height: 1.8;
}
.camera-info h2 {
  font-size: 23px;
}
@media (max-width: 1200px) {
  .invoice-layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .invoice-payment {
    position: static;
  }
  .invoice-payment .cart-bottom {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px 24px;
  }
  .invoice-payment .total,
  .invoice-payment .payment-note {
    grid-column: 1/-1;
  }
}
@media (max-width: 650px) {
  .invoice-editor .page-head {
    align-items: flex-start;
    gap: 14px;
    flex-direction: column;
  }
  .invoice-search {
    padding: 18px;
  }
  .invoice-sheet-heading {
    padding: 18px;
    flex-wrap: wrap;
  }
  .invoice-columns {
    display: none;
  }
  .invoice-line {
    grid-template-columns: minmax(0, 1fr) auto 36px;
    gap: 14px 8px;
    padding: 18px;
  }
  .line-product {
    grid-column: 1/3;
  }
  .remove-line {
    grid-column: 3;
    grid-row: 1;
  }
  .invoice-line .stepper {
    grid-column: 1;
    grid-row: 2;
  }
  .line-total {
    grid-column: 2/4;
    grid-row: 2;
  }
  .invoice-empty {
    min-height: 240px;
  }
  .invoice-footnote {
    padding: 16px 18px;
  }
  .invoice-payment .cart-bottom {
    display: block;
  }
  .invoice-payment label {
    margin-top: 14px;
  }
  .camera-trigger {
    font-size: 12px;
  }
  .result-price {
    max-width: 100px;
  }
  .invoice-search .search-bar {
    padding: 10px;
  }
  .search-bar input {
    font-size: 13px;
  }
  .search-bar button {
    padding: 9px;
    font-size: 11px;
  }
  .invoice-payment .cart-bottom input,
  .invoice-payment .cart-bottom select {
    font-size: 16px;
  }
}
</style>
