import type { CatalogProduct, ReceiptDraft } from "./inventory";

export type ReceiptField = keyof ReceiptDraft;
export type ReceiptErrors = Partial<Record<ReceiptField, string>>;

const quantityPattern = /^(0|[1-9]\d{0,10})(\.\d{1,3})?$/;

function milli(value: string): number | undefined {
  if (!quantityPattern.test(value)) return undefined;
  const [whole, decimal = ""] = value.split(".");
  return Number(whole) * 1000 + Number(decimal.padEnd(3, "0"));
}

export function validateReceipt(draft: ReceiptDraft, product?: CatalogProduct): ReceiptErrors {
  const errors: ReceiptErrors = {};
  if (!draft.supplierId) errors.supplierId = "Chọn nhà cung cấp đang hoạt động.";
  if (!draft.productId) errors.productId = "Chọn sản phẩm đang hoạt động.";

  const expected = milli(draft.expectedQuantity);
  const delivered = milli(draft.deliveredQuantity);
  const accepted = milli(draft.acceptedQuantity);
  const rejected = milli(draft.rejectedQuantity);
  if (expected === undefined || expected < 1)
    errors.expectedQuantity = "Nhập số lớn hơn 0, tối đa 3 chữ số thập phân.";
  if (delivered === undefined || delivered < 1)
    errors.deliveredQuantity = "Nhập số lớn hơn 0, tối đa 3 chữ số thập phân.";
  if (accepted === undefined)
    errors.acceptedQuantity = "Nhập số không âm, tối đa 3 chữ số thập phân.";
  if (rejected === undefined)
    errors.rejectedQuantity = "Nhập số không âm, tối đa 3 chữ số thập phân.";
  if (
    delivered !== undefined &&
    accepted !== undefined &&
    rejected !== undefined &&
    accepted + rejected !== delivered
  ) {
    errors.acceptedQuantity = "Số nhận + số từ chối phải bằng số giao.";
    errors.rejectedQuantity = "Số nhận + số từ chối phải bằng số giao.";
  }
  const unitCost = Number(draft.unitCost);
  if (!/^\d+$/.test(draft.unitCost) || !Number.isSafeInteger(unitCost))
    errors.unitCost = "Giá nhập phải là số nguyên VND không âm trong giới hạn an toàn.";
  if (product?.tracksExpiry && !draft.expiryDate)
    errors.expiryDate = "Sản phẩm này bắt buộc có hạn sử dụng.";
  if (draft.expiryDate && draft.expiryDate < vietnamBusinessDate())
    errors.expiryDate = "Hạn sử dụng không được trước ngày nhận hàng.";
  if (
    expected !== undefined &&
    delivered !== undefined &&
    rejected !== undefined &&
    (expected !== delivered || rejected > 0) &&
    !draft.discrepancyReason.trim()
  ) {
    errors.discrepancyReason = "Ghi lý do khi giao sai dự kiến hoặc có hàng bị từ chối.";
  }
  return errors;
}

export function vietnamBusinessDate(date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${value.year}-${value.month}-${value.day}`;
}
