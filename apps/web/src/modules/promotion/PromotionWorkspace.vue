<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ApiPromotionAdapter, type Promotion, type PromotionDraft } from "./api-promotion-adapter";
import type { CatalogProduct, InventoryBatch } from "../inventory/domain/inventory";

const props = defineProps<{
  adapter: ApiPromotionAdapter;
  canManage: boolean;
  online: boolean;
}>();

const promotions = ref<Promotion[]>([]);
const selected = ref<Promotion>();
const products = ref<CatalogProduct[]>([]);
const batches = ref<InventoryBatch[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const success = ref("");
const formVisible = ref(false);
const editingId = ref<string>();
const scopeKind = ref<"ALL" | "PRODUCT" | "BATCH">("ALL");
const targetKind = ref<"PRODUCT" | "BATCH">("PRODUCT");
const targetId = ref("");
const newTargetId = ref("");

const initialForm = () => ({
  code: "",
  name: "",
  discountType: "PERCENT" as PromotionDraft["discountType"],
  discountValue: "10",
  startsAt: localInput(new Date()),
  endsAt: localInput(new Date(Date.now() + 7 * 86400000)),
  status: "DRAFT" as PromotionDraft["status"],
});
const form = reactive(initialForm());

const choices = computed(() =>
  targetKind.value === "PRODUCT"
    ? products.value.map((p) => ({ id: p.id, label: `${p.sku} · ${p.name}` }))
    : batches.value.map((b) => ({
        id: b.batchId,
        label: `${b.batchNumber} · ${productName(b.productId)}`,
      })),
);
const createChoices = computed(() =>
  scopeKind.value === "PRODUCT"
    ? products.value.map((p) => ({ id: p.id, label: `${p.sku} · ${p.name}` }))
    : batches.value.map((b) => ({
        id: b.batchId,
        label: `${b.batchNumber} · ${productName(b.productId)}`,
      })),
);

onMounted(() => void load());

function localInput(date: Date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function productName(id: string) {
  return products.value.find((p) => p.id === id)?.name || id;
}

function batchName(id: string) {
  const batch = batches.value.find((b) => b.batchId === id);
  return batch ? `${batch.batchNumber} · ${productName(batch.productId)}` : id;
}

function discountText(promotion: Promotion) {
  return promotion.discountType === "PERCENT"
    ? `${promotion.discountValue}%`
    : `${new Intl.NumberFormat("vi-VN").format(promotion.discountValue)} đ/đơn vị`;
}

function dateText(value: string) {
  return new Date(value).toLocaleString("vi-VN");
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    promotions.value = await props.adapter.list();
    if (props.canManage) {
      const choices = await props.adapter.loadChoices();
      products.value = choices.products;
      batches.value = choices.batches;
    }
    if (selected.value) selected.value = await props.adapter.get(selected.value.id);
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    loading.value = false;
  }
}

async function selectPromotion(id: string) {
  error.value = "";
  success.value = "";
  try {
    selected.value = await props.adapter.get(id);
    targetKind.value = selected.value.batchIds.length ? "BATCH" : "PRODUCT";
    newTargetId.value = "";
    formVisible.value = false;
  } catch (cause) {
    error.value = (cause as Error).message;
  }
}

function startCreate() {
  Object.assign(form, initialForm());
  editingId.value = undefined;
  scopeKind.value = "ALL";
  targetId.value = "";
  selected.value = undefined;
  error.value = "";
  success.value = "";
  formVisible.value = true;
}

function startEdit() {
  if (!selected.value) return;
  const promotion = selected.value;
  Object.assign(form, {
    code: promotion.code,
    name: promotion.name,
    discountType: promotion.discountType,
    discountValue: String(promotion.discountValue),
    startsAt: localInput(new Date(promotion.startsAt)),
    endsAt: localInput(new Date(promotion.endsAt)),
    status: promotion.status,
  });
  editingId.value = promotion.id;
  formVisible.value = true;
  error.value = "";
  success.value = "";
}

function validatedDraft(): PromotionDraft {
  const value = form.discountValue.trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(value) || Number(value) <= 0)
    throw new Error("Mức giảm phải lớn hơn 0 và có tối đa hai chữ số thập phân.");
  if (form.discountType === "PERCENT" && Number(value) > 100)
    throw new Error("Mức giảm phần trăm không được vượt quá 100%.");
  if (form.discountType === "AMOUNT" && !/^\d+$/.test(value))
    throw new Error("Mức giảm VND phải là số nguyên.");
  const startsAt = new Date(form.startsAt);
  const endsAt = new Date(form.endsAt);
  if (
    !Number.isFinite(startsAt.getTime()) ||
    !Number.isFinite(endsAt.getTime()) ||
    endsAt <= startsAt
  )
    throw new Error("Thời điểm kết thúc phải sau thời điểm bắt đầu.");
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    discountType: form.discountType,
    discountValue: Number(value),
    startsAt: startsAt.toISOString(),
    endsAt: endsAt.toISOString(),
    status: form.status,
  };
}

async function save() {
  if (saving.value || !props.online || !props.canManage) return;
  saving.value = true;
  error.value = "";
  success.value = "";
  try {
    const draft = validatedDraft();
    if (editingId.value) {
      selected.value = await props.adapter.update(editingId.value, draft);
    } else {
      if (scopeKind.value !== "ALL" && !targetId.value)
        throw new Error("Chọn sản phẩm hoặc lô trước khi tạo khuyến mãi.");
      const requestedStatus = draft.status;
      const created = await props.adapter.create({
        ...draft,
        status: scopeKind.value === "ALL" ? requestedStatus : "DRAFT",
      });
      selected.value = created;
      if (scopeKind.value !== "ALL") {
        await props.adapter.addTarget(created.id, scopeKind.value, targetId.value);
        if (requestedStatus !== "DRAFT") {
          await props.adapter.update(created.id, draft);
        }
      }
      selected.value = await props.adapter.get(created.id);
    }
    promotions.value = await props.adapter.list();
    formVisible.value = false;
    success.value = "Đã lưu khuyến mãi.";
  } catch (cause) {
    error.value = (cause as Error).message;
    if (selected.value) {
      promotions.value = await props.adapter.list().catch(() => promotions.value);
    }
  } finally {
    saving.value = false;
  }
}

async function changeTarget(kind: "PRODUCT" | "BATCH", id: string, remove: boolean) {
  if (!selected.value || saving.value || !props.online || !props.canManage) return;
  saving.value = true;
  error.value = "";
  success.value = "";
  try {
    if (remove) await props.adapter.removeTarget(selected.value.id, kind, id);
    else await props.adapter.addTarget(selected.value.id, kind, id);
    selected.value = await props.adapter.get(selected.value.id);
    newTargetId.value = "";
    success.value = "Đã cập nhật phạm vi áp dụng.";
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">CHƯƠNG TRÌNH CỦA CỬA HÀNG</p>
      <h1>Khuyến mãi</h1>
      <p>Chọn sản phẩm hoặc lô và thời gian áp dụng. Giá bán được tính tại quầy.</p>
    </div>
    <button v-if="canManage" class="primary" :disabled="!online || saving" @click="startCreate">
      ＋ Tạo khuyến mãi
    </button>
  </div>

  <p v-if="error" role="alert" class="error">{{ error }}</p>
  <p v-if="success" role="status">{{ success }}</p>
  <p v-if="loading">Đang tải khuyến mãi...</p>
  <div v-else class="promotion-workspace">
    <section class="card">
      <h2>Chương trình</h2>
      <p v-if="!promotions.length">Chưa có khuyến mãi trong cửa hàng.</p>
      <button
        v-for="promotion in promotions"
        :key="promotion.id"
        class="promotion-item"
        :aria-pressed="selected?.id === promotion.id"
        @click="selectPromotion(promotion.id)"
      >
        <span
          ><strong>{{ promotion.name }}</strong
          ><small>{{ promotion.code }}</small></span
        >
        <span>{{ discountText(promotion) }} · {{ promotion.status }}</span>
      </button>
    </section>

    <section v-if="selected && !formVisible" class="card promotion-details">
      <h2>{{ selected.name }}</h2>
      <p>{{ selected.code }} · {{ discountText(selected) }} · {{ selected.status }}</p>
      <p>Từ {{ dateText(selected.startsAt) }} đến {{ dateText(selected.endsAt) }}</p>
      <button v-if="canManage" class="secondary" :disabled="!online || saving" @click="startEdit">
        Sửa chương trình
      </button>
      <h3>Phạm vi áp dụng</h3>
      <p v-if="!selected.productIds.length && !selected.batchIds.length">Toàn bộ sản phẩm.</p>
      <div v-for="id in selected.productIds" :key="id" class="promotion-target">
        <span>Sản phẩm: {{ productName(id) }}</span>
        <button
          v-if="canManage"
          :disabled="!online || saving"
          @click="changeTarget('PRODUCT', id, true)"
        >
          Bỏ
        </button>
      </div>
      <div v-for="id in selected.batchIds" :key="id" class="promotion-target">
        <span>Lô: {{ batchName(id) }}</span>
        <button
          v-if="canManage"
          :disabled="!online || saving"
          @click="changeTarget('BATCH', id, true)"
        >
          Bỏ
        </button>
      </div>
      <div v-if="canManage" class="promotion-target-form">
        <label
          >Loại phạm vi
          <select
            v-model="targetKind"
            :disabled="selected.productIds.length > 0 || selected.batchIds.length > 0"
          >
            <option value="PRODUCT">Sản phẩm</option>
            <option value="BATCH">Lô</option>
          </select>
        </label>
        <label
          >Thêm vào phạm vi
          <select v-model="newTargetId">
            <option value="">Chọn {{ targetKind === "PRODUCT" ? "sản phẩm" : "lô" }}</option>
            <option v-for="choice in choices" :key="choice.id" :value="choice.id">
              {{ choice.label }}
            </option>
          </select>
        </label>
        <button
          class="secondary"
          :disabled="!online || saving || !newTargetId"
          @click="changeTarget(targetKind, newTargetId, false)"
        >
          Thêm
        </button>
      </div>
    </section>

    <section v-if="formVisible && canManage" class="card promotion-details">
      <h2>{{ editingId ? "Sửa khuyến mãi" : "Tạo khuyến mãi" }}</h2>
      <form class="promotion-form" @submit.prevent="save">
        <label>Mã <input v-model="form.code" required maxlength="60" /></label>
        <label>Tên <input v-model="form.name" required maxlength="200" /></label>
        <label
          >Loại giảm
          <select v-model="form.discountType">
            <option value="PERCENT">Phần trăm</option>
            <option value="AMOUNT">VND trên đơn vị</option>
          </select>
        </label>
        <label
          >Mức giảm
          <input
            v-model="form.discountValue"
            required
            type="number"
            min="0.01"
            :step="form.discountType === 'PERCENT' ? '0.01' : '1'"
        /></label>
        <label>Bắt đầu <input v-model="form.startsAt" required type="datetime-local" /></label>
        <label>Kết thúc <input v-model="form.endsAt" required type="datetime-local" /></label>
        <label
          >Trạng thái
          <select v-model="form.status">
            <option value="DRAFT">Nháp</option>
            <option value="ACTIVE">Đang áp dụng</option>
            <option value="INACTIVE">Tạm ngừng</option>
          </select>
        </label>
        <template v-if="!editingId">
          <label
            >Phạm vi
            <select v-model="scopeKind" @change="targetId = ''">
              <option value="ALL">Toàn bộ sản phẩm</option>
              <option value="PRODUCT">Một sản phẩm</option>
              <option value="BATCH">Một lô</option>
            </select>
          </label>
          <label v-if="scopeKind !== 'ALL'"
            >Chọn {{ scopeKind === "PRODUCT" ? "sản phẩm" : "lô" }}
            <select v-model="targetId" required>
              <option value="">Chọn phạm vi</option>
              <option v-for="choice in createChoices" :key="choice.id" :value="choice.id">
                {{ choice.label }}
              </option>
            </select>
          </label>
        </template>
        <div class="promotion-actions">
          <button class="primary" :disabled="!online || saving">Lưu khuyến mãi</button>
          <button type="button" class="secondary" @click="formVisible = false">Hủy</button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.promotion-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(300px, 1.5fr);
  gap: 20px;
}
.promotion-workspace .card {
  padding: 24px;
}
.promotion-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid #eee;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.promotion-item[aria-pressed="true"] {
  color: #795788;
}
.promotion-item span:first-child {
  display: grid;
  gap: 4px;
}
.promotion-item small {
  color: #72617d;
}
.promotion-details {
  display: grid;
  align-content: start;
  gap: 12px;
}
.promotion-target {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.promotion-target-form,
.promotion-form {
  display: grid;
  gap: 12px;
}
.promotion-target-form label,
.promotion-form label {
  display: grid;
  gap: 5px;
}
.promotion-target-form select,
.promotion-form select,
.promotion-form input {
  width: 100%;
}
.promotion-actions {
  display: flex;
  gap: 10px;
}
@media (max-width: 760px) {
  .promotion-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
