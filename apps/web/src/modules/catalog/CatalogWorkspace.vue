<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import ProductImage from "../sale/presentation/ProductImage.vue";
import {
  type Barcode,
  type CatalogKind,
  type CatalogStatus,
  type Category,
  type Product,
  type Supplier,
  type Unit,
  ApiCatalogAdapter,
} from "./api-catalog-adapter";

const props = defineProps<{
  adapter: ApiCatalogAdapter;
  entry: "products" | "suppliers";
  canWrite: boolean;
  online: boolean;
}>();

const sections: Array<{ id: CatalogKind; label: string; singular: string }> = [
  { id: "products", label: "Sản phẩm", singular: "sản phẩm" },
  { id: "categories", label: "Danh mục", singular: "danh mục" },
  { id: "units", label: "Đơn vị tính", singular: "đơn vị tính" },
  { id: "suppliers", label: "Nhà cung cấp", singular: "nhà cung cấp" },
];
const section = ref<CatalogKind>(props.entry);
const sectionMeta = computed(() => sections.find((item) => item.id === section.value)!);
const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const units = ref<Unit[]>([]);
const suppliers = ref<Supplier[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const success = ref("");
const query = ref("");
const editingId = ref<string>();
const formOpen = ref(false);
const productForBarcodes = ref<Product>();
const barcodes = ref<Barcode[]>([]);
const barcodeLoading = ref(false);
const barcodeEditingId = ref<string>();
const barcodeFormOpen = ref(false);
const barcodeForm = reactive({ barcode: "", isPrimary: false });
/** File ảnh mới được chọn, chờ upload sau khi lưu sản phẩm. */
const imageFile = ref<File | null>(null);
/** URL preview cục bộ (ObjectURL) hoặc URL từ máy chủ của ảnh hiện tại. */
const imagePreview = ref<string | null>(null);
const imageUploading = ref(false);
const imageRemoved = ref(false);
const form = reactive({
  code: "",
  sku: "",
  name: "",
  parentId: "",
  categoryId: "",
  baseUnitId: "",
  precisionScale: 0,
  tracksExpiry: false,
  status: "ACTIVE" as CatalogStatus,
  phone: "",
  email: "",
});

const rows = computed(() => {
  const records =
    section.value === "products"
      ? products.value
      : section.value === "categories"
        ? categories.value
        : section.value === "units"
          ? units.value
          : suppliers.value;
  const term = query.value.trim().toLocaleLowerCase("vi-VN");
  return term
    ? records.filter((record) =>
        Object.values(record).some(
          (value) => typeof value === "string" && value.toLocaleLowerCase("vi-VN").includes(term),
        ),
      )
    : records;
});

const categoryName = (id: string | null) =>
  id ? categories.value.find((item) => item.id === id)?.name || id : "—";
const unitCode = (id: string) => units.value.find((item) => item.id === id)?.code || id;
const canAct = computed(() => props.canWrite && props.online && !saving.value);

function clearMessages() {
  error.value = "";
  success.value = "";
}

function resetEditor() {
  editingId.value = undefined;
  formOpen.value = false;
  barcodeEditingId.value = undefined;
  barcodeFormOpen.value = false;
  productForBarcodes.value = undefined;
  barcodes.value = [];
  resetImageState();
}

function resetImageState() {
  if (imagePreview.value && imagePreview.value.startsWith("blob:")) {
    URL.revokeObjectURL(imagePreview.value);
  }
  imageFile.value = null;
  imagePreview.value = null;
  imageRemoved.value = false;
}

function onImagePicked(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    error.value = "Ảnh không được vượt quá 5 MB.";
    input.value = "";
    return;
  }
  // Giải phóng ObjectURL cũ nếu có.
  if (imagePreview.value && imagePreview.value.startsWith("blob:")) {
    URL.revokeObjectURL(imagePreview.value);
  }
  imageFile.value = file;
  imagePreview.value = URL.createObjectURL(file);
  imageRemoved.value = false;
  error.value = "";
}

function removeSelectedImage() {
  if (imagePreview.value && imagePreview.value.startsWith("blob:")) {
    URL.revokeObjectURL(imagePreview.value);
  }
  imageFile.value = null;
  imagePreview.value = null;
  imageRemoved.value = true;
}

async function load() {
  if (!props.online) {
    error.value = "Cần kết nối mạng để tải danh mục từ máy chủ.";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    switch (section.value) {
      case "products": {
        const [nextProducts, nextCategories, nextUnits] = await Promise.all([
          props.adapter.list("products"),
          props.adapter.list("categories"),
          props.adapter.list("units"),
        ]);
        products.value = nextProducts;
        categories.value = nextCategories;
        units.value = nextUnits;
        break;
      }
      case "categories":
        categories.value = await props.adapter.list("categories");
        break;
      case "units":
        units.value = await props.adapter.list("units");
        break;
      case "suppliers":
        suppliers.value = await props.adapter.list("suppliers");
        break;
    }
    if (productForBarcodes.value && section.value === "products") {
      const current = products.value.find((item) => item.id === productForBarcodes.value?.id);
      if (current) {
        productForBarcodes.value = current;
        barcodes.value = await props.adapter.listBarcodes(current.id);
      } else {
        productForBarcodes.value = undefined;
        barcodes.value = [];
      }
    }
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    loading.value = false;
  }
}

function chooseSection(next: CatalogKind) {
  if (section.value === next) return;
  section.value = next;
  query.value = "";
  resetEditor();
  clearMessages();
  void load();
}

watch(
  () => props.entry,
  (entry) => {
    section.value = entry;
    query.value = "";
    resetEditor();
    clearMessages();
    void load();
  },
  { immediate: true },
);

function startCreate() {
  if (!canAct.value) return;
  Object.assign(form, {
    code: "",
    sku: "",
    name: "",
    parentId: "",
    categoryId: "",
    baseUnitId: "",
    precisionScale: 0,
    tracksExpiry: false,
    status: "ACTIVE",
    phone: "",
    email: "",
  });
  editingId.value = undefined;
  formOpen.value = true;
  resetImageState();
  clearMessages();
}

async function startEdit(id: string) {
  if (!canAct.value) return;
  saving.value = true;
  clearMessages();
  try {
    const record = await props.adapter.get(section.value, id);
    Object.assign(form, {
      code: "",
      sku: "",
      name: "",
      parentId: "",
      categoryId: "",
      baseUnitId: "",
      precisionScale: 0,
      tracksExpiry: false,
      status: "ACTIVE",
      phone: "",
      email: "",
    });
    Object.assign(form, record);
    form.parentId = "parentId" in record ? record.parentId || "" : "";
    form.phone = "phone" in record ? record.phone || "" : "";
    form.email = "email" in record ? record.email || "" : "";
    // Hiển thị ảnh hiện tại của sản phẩm (nếu có) để người dùng biết.
    resetImageState();
    if (section.value === "products") {
      imagePreview.value = (record as import("./api-catalog-adapter").Product).imageUrl ?? null;
    }
    editingId.value = id;
    formOpen.value = true;
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
  }
}

async function save() {
  if (!canAct.value || !formOpen.value) return;
  saving.value = true;
  clearMessages();
  try {
    const code = form.code.trim();
    const name = form.name.trim();
    let savedProductId: string | undefined;
    switch (section.value) {
      case "products": {
        const saved = await props.adapter.save("products", editingId.value, {
          sku: form.sku.trim(),
          name,
          categoryId: form.categoryId,
          baseUnitId: form.baseUnitId,
          tracksExpiry: form.tracksExpiry,
          status: form.status,
        });
        // Khi tạo mới: dùng id trả về từ server.
        // Khi sửa: fallback về editingId.value nếu server trả 204.
        savedProductId = saved?.id ?? editingId.value;
        break;
      }
      case "categories":
        await props.adapter.save("categories", editingId.value, {
          code,
          name,
          parentId: form.parentId || null,
          status: form.status,
        });
        break;
      case "units":
        await props.adapter.save("units", editingId.value, {
          code,
          name,
          precisionScale: Number(form.precisionScale),
        });
        break;
      case "suppliers":
        await props.adapter.save("suppliers", editingId.value, {
          code,
          name,
          phone: form.phone.trim() || null,
          email: form.email.trim() || null,
          status: form.status,
        });
        break;
    }
    // Xử lý ảnh sản phẩm: upload ảnh mới hoặc xóa ảnh cũ nếu người dùng đã gỡ bỏ.
    if (imageFile.value && savedProductId) {
      imageUploading.value = true;
      try {
        await props.adapter.uploadImage(savedProductId, imageFile.value);
      } catch (uploadErr) {
        success.value = "";
        error.value = `Đã lưu thông tin sản phẩm nhưng không thể tải ảnh lên: ${(uploadErr as Error).message}`;
        return;
      } finally {
        imageUploading.value = false;
      }
    } else if (imageRemoved.value && savedProductId) {
      try {
        await props.adapter.deleteImage(savedProductId);
      } catch (delErr) {
        const msg = (delErr as Error).message || "";
        // 404 nghĩa là trên server vốn chưa có ảnh hoặc đã bị xóa trước đó, không coi là lỗi
        if (!msg.includes("404")) {
          success.value = "";
          error.value = `Đã lưu thông tin sản phẩm nhưng không thể xóa ảnh: ${msg}`;
          return;
        }
      }
    }

    formOpen.value = false;
    editingId.value = undefined;
    resetImageState();
    await load();
    if (!error.value) success.value = "Đã lưu thay đổi vào máy chủ.";
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
    imageUploading.value = false;
  }
}

async function remove(id: string, name: string) {
  if (!canAct.value || !window.confirm(`Xóa ${sectionMeta.value.singular} “${name}”?`)) return;
  saving.value = true;
  clearMessages();
  try {
    await props.adapter.remove(section.value, id);
    if (editingId.value === id) formOpen.value = false;
    await load();
    if (!error.value) success.value = "Đã xóa bản ghi.";
  } catch (cause) {
    const detail = (cause as Error).message;
    error.value =
      section.value === "units"
        ? detail
        : `${detail} Nếu dữ liệu đã được sử dụng, hãy chuyển trạng thái sang Ngừng sử dụng.`;
  } finally {
    saving.value = false;
  }
}

async function showBarcodes(product: Product) {
  productForBarcodes.value = product;
  barcodes.value = [];
  barcodeFormOpen.value = false;
  barcodeEditingId.value = undefined;
  barcodeLoading.value = true;
  clearMessages();
  try {
    barcodes.value = await props.adapter.listBarcodes(product.id);
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    barcodeLoading.value = false;
  }
}

function startBarcodeCreate() {
  if (!canAct.value) return;
  Object.assign(barcodeForm, { barcode: "", isPrimary: false });
  barcodeEditingId.value = undefined;
  barcodeFormOpen.value = true;
  clearMessages();
}

async function startBarcodeEdit(barcodeId: string) {
  if (!canAct.value || !productForBarcodes.value) return;
  saving.value = true;
  clearMessages();
  try {
    const current = await props.adapter.getBarcode(productForBarcodes.value.id, barcodeId);
    Object.assign(barcodeForm, { barcode: current.barcode, isPrimary: current.isPrimary });
    barcodeEditingId.value = current.id;
    barcodeFormOpen.value = true;
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
  }
}

async function saveBarcode() {
  if (!canAct.value || !productForBarcodes.value || !barcodeFormOpen.value) return;
  saving.value = true;
  clearMessages();
  try {
    await props.adapter.saveBarcode(productForBarcodes.value.id, barcodeEditingId.value, {
      barcode: barcodeForm.barcode.trim(),
      isPrimary: barcodeForm.isPrimary,
    });
    barcodes.value = await props.adapter.listBarcodes(productForBarcodes.value.id);
    barcodeFormOpen.value = false;
    barcodeEditingId.value = undefined;
    success.value = "Đã lưu mã vạch.";
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
  }
}

async function removeBarcode(barcode: Barcode) {
  if (
    !canAct.value ||
    !productForBarcodes.value ||
    !window.confirm(`Xóa mã vạch “${barcode.barcode}”?`)
  )
    return;
  saving.value = true;
  clearMessages();
  try {
    await props.adapter.removeBarcode(productForBarcodes.value.id, barcode.id);
    barcodes.value = await props.adapter.listBarcodes(productForBarcodes.value.id);
    if (barcodeEditingId.value === barcode.id) barcodeFormOpen.value = false;
    success.value = "Đã xóa mã vạch.";
  } catch (cause) {
    error.value = (cause as Error).message;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="catalog-workspace">
    <div class="page-head">
      <div>
        <h1>Quản lý hàng hóa</h1>
        <p>Sản phẩm, danh mục, đơn vị và nhà cung cấp trong cùng tổ chức.</p>
      </div>
      <button v-if="canWrite" class="primary" :disabled="!canAct" @click="startCreate">
        Thêm {{ sectionMeta.singular }}
      </button>
    </div>

    <div class="catalog-tabs" role="tablist" aria-label="Loại dữ liệu hàng hóa">
      <button
        v-for="item in sections"
        :key="item.id"
        type="button"
        role="tab"
        :aria-selected="section === item.id"
        :class="{ selected: section === item.id }"
        @click="chooseSection(item.id)"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="catalog-toolbar">
      <label>
        <span class="sr-only">Tìm trong {{ sectionMeta.label }}</span>
        <input v-model="query" type="search" :placeholder="`Tìm ${sectionMeta.singular}…`" />
      </label>
      <button type="button" class="secondary" :disabled="loading || !online" @click="load">
        Tải lại
      </button>
    </div>
    <p v-if="!online" class="catalog-notice">
      Bạn đang ngoại tuyến. Kết nối lại để xem hoặc sửa dữ liệu.
    </p>
    <p v-if="!canWrite" class="catalog-notice">Tài khoản này có quyền xem danh mục.</p>
    <p v-if="error" class="catalog-error" role="alert">{{ error }}</p>
    <p v-if="success" class="catalog-success" role="status">{{ success }}</p>

    <div class="card table-wrap catalog-table">
      <table>
        <thead>
          <tr v-if="section === 'products'">
            <th>Sản phẩm</th>
            <th>SKU</th>
            <th>Danh mục</th>
            <th>Đơn vị</th>
            <th>Giá bán</th>
            <th>Khả dụng</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
          <tr v-else-if="section === 'categories'">
            <th>Danh mục</th>
            <th>Mã</th>
            <th>Danh mục cha</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
          <tr v-else-if="section === 'units'">
            <th>Đơn vị tính</th>
            <th>Mã</th>
            <th>Chữ số thập phân</th>
            <th>Thao tác</th>
          </tr>
          <tr v-else>
            <th>Nhà cung cấp</th>
            <th>Mã</th>
            <th>Điện thoại</th>
            <th>Email</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody v-if="!loading">
          <tr v-for="record in rows" :key="record.id">
            <template v-if="section === 'products'">
              <td>
                <div class="product-cell">
                  <span>
                    <ProductImage
                      :sku="(record as Product).sku"
                      :name="record.name"
                      :image-url="(record as Product).imageUrl"
                    />
                  </span>
                  <b>{{ record.name }}</b>
                </div>
              </td>
              <td class="catalog-code">{{ (record as Product).sku }}</td>
              <td>{{ categoryName((record as Product).categoryId) }}</td>
              <td>{{ unitCode((record as Product).baseUnitId) }}</td>
              <td>Theo báo giá</td>
              <td><span class="status">—</span></td>
              <td>{{ (record as Product).status === "ACTIVE" ? "Đang bán" : "Ngừng bán" }}</td>
            </template>
            <template v-else-if="section === 'categories'">
              <td>
                <b>{{ record.name }}</b>
              </td>
              <td class="catalog-code">{{ (record as Category).code }}</td>
              <td>{{ categoryName((record as Category).parentId) }}</td>
              <td>{{ (record as Category).status === "ACTIVE" ? "Đang dùng" : "Ngừng dùng" }}</td>
            </template>
            <template v-else-if="section === 'units'">
              <td>
                <b>{{ record.name }}</b>
              </td>
              <td class="catalog-code">{{ (record as Unit).code }}</td>
              <td>{{ (record as Unit).precisionScale }}</td>
            </template>
            <template v-else>
              <td>
                <b>{{ record.name }}</b>
              </td>
              <td class="catalog-code">{{ (record as Supplier).code }}</td>
              <td>{{ (record as Supplier).phone || "—" }}</td>
              <td>{{ (record as Supplier).email || "—" }}</td>
              <td>
                {{ (record as Supplier).status === "ACTIVE" ? "Đang hợp tác" : "Ngừng hợp tác" }}
              </td>
            </template>
            <td>
              <div class="catalog-actions">
                <button
                  v-if="section === 'products'"
                  type="button"
                  @click="showBarcodes(record as Product)"
                >
                  Mã vạch
                </button>
                <button
                  v-if="canWrite"
                  type="button"
                  :disabled="!canAct"
                  @click="startEdit(record.id)"
                >
                  Sửa
                </button>
                <button
                  v-if="canWrite"
                  type="button"
                  :disabled="!canAct"
                  @click="remove(record.id, record.name)"
                >
                  Xóa
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-empty" role="status">
        Đang tải {{ sectionMeta.label.toLocaleLowerCase("vi-VN") }}…
      </div>
      <div v-else-if="!error && !rows.length" class="catalog-empty">
        {{ query ? "Không tìm thấy kết quả phù hợp." : `Chưa có ${sectionMeta.singular} nào.` }}
        <button v-if="!query && canAct" type="button" @click="startCreate">
          Tạo {{ sectionMeta.singular }} đầu tiên
        </button>
      </div>
    </div>

    <form v-if="formOpen && canWrite" class="catalog-editor card" @submit.prevent="save">
      <div class="catalog-panel-head">
        <div>
          <h2>{{ editingId ? "Sửa" : "Thêm" }} {{ sectionMeta.singular }}</h2>
          <p>Những trường bắt buộc được đánh dấu *.</p>
        </div>
        <button type="button" class="text-button" @click="formOpen = false">Đóng</button>
      </div>
      <div class="catalog-fields">
        <label v-if="section !== 'products'"
          >Mã *<input v-model="form.code" required :maxlength="section === 'units' ? 20 : 40"
        /></label>
        <label v-if="section === 'products'"
          >SKU *<input v-model="form.sku" required maxlength="80"
        /></label>
        <label
          >Tên *<input v-model="form.name" required :maxlength="section === 'units' ? 50 : 200"
        /></label>
        <label v-if="section === 'products'"
          >Danh mục *
          <select v-model="form.categoryId" required>
            <option value="" disabled>Chọn danh mục</option>
            <option v-for="item in categories" :key="item.id" :value="item.id">
              {{ item.name }}
            </option>
          </select>
        </label>
        <label v-if="section === 'products'"
          >Đơn vị tính *
          <select v-model="form.baseUnitId" required>
            <option value="" disabled>Chọn đơn vị</option>
            <option v-for="item in units" :key="item.id" :value="item.id">
              {{ item.code }} · {{ item.name }}
            </option>
          </select>
        </label>
        <label v-if="section === 'categories'"
          >Danh mục cha
          <select v-model="form.parentId">
            <option value="">Không có</option>
            <option
              v-for="item in categories.filter((value) => value.id !== editingId)"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </option>
          </select>
        </label>
        <label v-if="section === 'units'"
          >Chữ số thập phân *<input
            v-model.number="form.precisionScale"
            type="number"
            min="0"
            max="3"
            step="1"
            required
        /></label>
        <label v-if="section === 'suppliers'"
          >Điện thoại<input v-model="form.phone" type="tel" maxlength="40"
        /></label>
        <label v-if="section === 'suppliers'"
          >Email<input v-model="form.email" type="email" maxlength="255"
        /></label>
        <label v-if="section !== 'units'"
          >Trạng thái
          <select v-model="form.status">
            <option value="ACTIVE">Đang dùng</option>
            <option value="INACTIVE">Ngừng dùng</option>
          </select>
        </label>
        <label v-if="section === 'products'" class="catalog-check"
          ><input v-model="form.tracksExpiry" type="checkbox" /> Theo dõi hạn sử dụng</label
        >
      </div>

      <!-- ===== Khu vực upload ảnh (chỉ hiển thị khi thêm/sửa sản phẩm) ===== -->
      <div v-if="section === 'products'" class="catalog-image-upload">
        <p class="catalog-image-label">
          Ảnh sản phẩm
          <span class="catalog-image-hint">(tuỳ chọn · JPEG, PNG, WebP · tối đa 5 MB)</span>
        </p>
        <div class="catalog-image-preview-wrap">
          <div class="catalog-image-preview">
            <img
              v-if="imagePreview"
              :src="imagePreview"
              alt="Xem trước ảnh sản phẩm"
              class="catalog-image-thumb"
            />
            <span v-else class="catalog-image-placeholder" aria-hidden="true">📷</span>
          </div>
          <div class="catalog-image-controls">
            <label class="catalog-image-pick secondary">
              <input
                id="product-image-input"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="sr-only"
                :disabled="!canAct"
                @change="onImagePicked"
              />
              {{ imagePreview ? "Đổi ảnh" : "Chọn ảnh" }}
            </label>
            <button
              v-if="imagePreview"
              type="button"
              class="catalog-image-remove"
              :disabled="!canAct"
              @click="removeSelectedImage"
            >
              Xóa ảnh
            </button>
          </div>
        </div>
        <p v-if="imageUploading" class="catalog-notice">Đang tải ảnh lên máy chủ…</p>
      </div>
      <!-- ===== Kết thúc upload ảnh ===== -->

      <p
        v-if="section === 'products' && (!categories.length || !units.length)"
        class="catalog-notice"
      >
        Tạo ít nhất một danh mục và đơn vị tính trước khi thêm sản phẩm.
      </p>
      <div class="catalog-form-actions">
        <button type="button" class="secondary" @click="formOpen = false">Hủy</button>
        <button
          class="primary"
          type="submit"
          :disabled="
            !canAct ||
            imageUploading ||
            (section === 'products' && (!categories.length || !units.length))
          "
        >
          {{ saving || imageUploading ? "Đang lưu…" : "Lưu thay đổi" }}
        </button>
      </div>
    </form>

    <section
      v-if="section === 'products' && productForBarcodes"
      class="catalog-barcodes card"
      aria-label="Mã vạch sản phẩm"
    >
      <div class="catalog-panel-head">
        <div>
          <h2>Mã vạch · {{ productForBarcodes.name }}</h2>
          <p>Mỗi sản phẩm chỉ có một mã vạch chính.</p>
        </div>
        <button type="button" class="text-button" @click="productForBarcodes = undefined">
          Đóng
        </button>
      </div>
      <p v-if="barcodeLoading" role="status">Đang tải mã vạch…</p>
      <ul v-else class="catalog-barcode-list">
        <li v-for="item in barcodes" :key="item.id">
          <span class="catalog-code"
            >{{ item.barcode }} <small v-if="item.isPrimary">Chính</small></span
          >
          <div v-if="canWrite" class="catalog-actions">
            <button type="button" :disabled="!canAct" @click="startBarcodeEdit(item.id)">
              Sửa
            </button>
            <button type="button" :disabled="!canAct" @click="removeBarcode(item)">Xóa</button>
          </div>
        </li>
        <li v-if="!barcodes.length">Sản phẩm chưa có mã vạch.</li>
      </ul>
      <button
        v-if="canWrite && !barcodeFormOpen"
        type="button"
        class="secondary"
        :disabled="!canAct"
        @click="startBarcodeCreate"
      >
        Thêm mã vạch
      </button>
      <form
        v-if="canWrite && barcodeFormOpen"
        class="catalog-barcode-form"
        @submit.prevent="saveBarcode"
      >
        <label>Mã vạch *<input v-model="barcodeForm.barcode" required maxlength="100" /></label>
        <label class="catalog-check"
          ><input v-model="barcodeForm.isPrimary" type="checkbox" /> Mã vạch chính</label
        >
        <p>Nếu đã có mã chính, hãy bỏ chọn mã cũ trước khi đặt mã mới làm chính.</p>
        <div class="catalog-form-actions">
          <button type="button" class="secondary" @click="barcodeFormOpen = false">Hủy</button>
          <button type="submit" class="primary" :disabled="!canAct">
            {{ saving ? "Đang lưu…" : "Lưu mã vạch" }}
          </button>
        </div>
      </form>
    </section>
  </section>
</template>

<style scoped>
.catalog-workspace {
  display: grid;
  gap: 18px;
}
.catalog-workspace .page-head {
  margin-bottom: 4px;
}
.catalog-tabs {
  display: flex;
  gap: 5px;
  overflow-x: auto;
  border-bottom: 1px solid var(--line);
}
.catalog-tabs button {
  min-height: 44px;
  padding: 10px 16px;
  white-space: nowrap;
  color: var(--muted);
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  font-weight: 600;
}
.catalog-tabs button.selected {
  color: var(--purple);
  border-bottom-color: var(--purple);
}
.catalog-tabs button:hover {
  color: var(--purple);
  background: #f8f4fa;
}
.catalog-toolbar,
.catalog-panel-head,
.catalog-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}
.catalog-toolbar label {
  flex: 1;
  max-width: 430px;
}
.catalog-toolbar input {
  width: 100%;
}
.catalog-table {
  min-width: 0;
}
.catalog-table table {
  min-width: 750px;
}
.catalog-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.catalog-actions button,
.catalog-empty button {
  border: 0;
  background: none;
  color: var(--purple);
  font-weight: 600;
  font-size: 12px;
  padding: 8px 3px;
  white-space: nowrap;
}
.catalog-actions button:hover,
.catalog-empty button:hover {
  text-decoration: underline;
}
.catalog-code {
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}
.catalog-empty {
  padding: 30px 22px;
  color: var(--muted);
  display: flex;
  gap: 12px;
  align-items: center;
}
.catalog-editor,
.catalog-barcodes {
  padding: 24px;
}
.catalog-panel-head {
  align-items: flex-start;
  margin-bottom: 16px;
}
.catalog-panel-head h2 {
  margin: 0 0 5px;
}
.catalog-panel-head p {
  margin: 0;
}
.catalog-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.catalog-fields label,
.catalog-barcode-form label {
  display: grid;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #303036;
}
.catalog-fields input:not([type="checkbox"]),
.catalog-fields select,
.catalog-barcode-form input:not([type="checkbox"]) {
  width: 100%;
  min-height: 43px;
}
.catalog-check {
  display: flex !important;
  align-items: center;
  gap: 9px;
}
.catalog-check input {
  width: 18px;
  height: 18px;
}
.catalog-form-actions {
  justify-content: flex-end;
  margin-top: 20px;
}
.catalog-notice,
.catalog-error,
.catalog-success {
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 12px;
}
.catalog-notice {
  background: #f6f3f8;
  color: #635a6b;
}
.catalog-error {
  background: #fff0ee;
  color: #a32b22;
}
.catalog-success {
  background: #edf8ef;
  color: #236b34;
}
.catalog-barcode-list {
  padding: 0;
  margin: 0 0 14px;
  list-style: none;
}
.catalog-barcode-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 44px;
  padding: 6px 0;
  border-bottom: 1px solid var(--line);
}
.catalog-barcode-list small {
  margin-left: 8px;
  color: var(--purple);
}
.catalog-barcode-form {
  display: grid;
  gap: 12px;
  max-width: 480px;
  margin-top: 15px;
}
.catalog-workspace button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.catalog-workspace :is(button, input, select):focus-visible {
  outline: 2px solid var(--purple);
  outline-offset: 2px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@media (max-width: 700px) {
  .catalog-workspace .page-head {
    align-items: flex-start;
    flex-direction: column;
  }
  .catalog-toolbar {
    align-items: stretch;
  }
  .catalog-fields {
    grid-template-columns: 1fr;
  }
  .catalog-editor,
  .catalog-barcodes {
    padding: 18px;
  }
  .catalog-panel-head {
    gap: 8px;
  }
}

/* ===== Image upload ===== */
.catalog-image-upload {
  margin-top: 6px;
  border-top: 1px solid var(--line);
  padding-top: 18px;
}
.catalog-image-label {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: #303036;
}
.catalog-image-hint {
  font-weight: 400;
  color: var(--muted);
}
.catalog-image-preview-wrap {
  display: flex;
  align-items: center;
  gap: 18px;
}
.catalog-image-preview {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: #f6f2ed;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.catalog-image-thumb {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.catalog-image-placeholder {
  font-size: 32px;
  line-height: 1;
}
.catalog-image-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.catalog-image-pick {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #f8f4fa;
  font-size: 13px;
  font-weight: 600;
  color: var(--purple);
  cursor: pointer;
  text-align: center;
  transition: background 0.15s;
}
.catalog-image-pick:hover {
  background: #ede8f5;
}
.catalog-image-pick:has(input:disabled) {
  opacity: 0.55;
  cursor: not-allowed;
}
.catalog-image-remove {
  border: 0;
  background: none;
  color: #a32b22;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 0;
  text-align: left;
  cursor: pointer;
}
.catalog-image-remove:hover {
  text-decoration: underline;
}
.catalog-image-remove:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
