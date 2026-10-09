export type StocktakeStatus = "PENDING" | "REVIEW" | "APPROVED" | "CONFLICT";

export interface StocktakeRecord {
  id: string;
  countedAt: string;
  productId: string;
  batchId: string;
  expectedQuantity: number;
  actualQuantity: number;
  note: string;
  status: StocktakeStatus;
}

export interface StocktakeSubmission {
  batchId: string;
  actualQuantity: number;
  note: string;
}

export interface QuantityValidation {
  value?: number;
  normalized?: string;
  error?: string;
}

const MAX_QUANTITY = 99_999_999_999.999;

export function quantityScale(unitCode?: string): number {
  return unitCode?.trim().toLocaleUpperCase("vi") === "KG" ? 3 : 0;
}

export function validateStocktakeQuantity(raw: string, unitCode?: string): QuantityValidation {
  const normalized = raw.trim().replace(",", ".");
  const scale = quantityScale(unitCode);
  const format = scale === 0 ? /^\d+$/ : new RegExp(`^\\d+(?:\\.\\d{1,${scale}})?$`);

  if (!normalized) return { error: "Nhập số lượng thực tế." };
  if (!format.test(normalized)) {
    return {
      error:
        scale === 0
          ? "Đơn vị này chỉ nhận số nguyên không âm."
          : `Đơn vị KG nhận tối đa ${scale} chữ số thập phân.`,
    };
  }

  const value = Number(normalized);
  if (!Number.isFinite(value) || value < 0 || value > MAX_QUANTITY) {
    return { error: "Số lượng nằm ngoài giới hạn cho phép." };
  }

  return { value, normalized };
}
