export type StocktakeStatus = "PENDING" | "REVIEW" | "APPROVED" | "CONFLICT";

export interface StocktakeRecord {
  id: string;
  clientOperationId?: string;
  countedAt: string;
  productId: string;
  batchId: string;
  expectedQuantity: number;
  actualQuantity: number;
  note: string;
  status: StocktakeStatus;
  syncBlockedReason?: string;
}

export interface StocktakeSubmission {
  batchId: string;
  actualQuantity: number;
  note: string;
}

export interface StocktakeScope {
  actorId: string;
  organizationId: string;
  storeId: string;
}

export interface StocktakeQueueItem extends StocktakeScope {
  clientOperationId: string;
  countedAt: string;
  productId: string;
  batchId: string;
  expectedQuantity: number;
  actualQuantity: number;
  note: string;
  status: Extract<StocktakeStatus, "PENDING" | "CONFLICT">;
  conflictReason?: string;
}

export interface StocktakeRetryContext extends StocktakeScope {
  canSync: boolean;
}

export interface StocktakeRetryDecision {
  allowed: boolean;
  reason?: string;
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

export function sameStocktakeDraft(
  item: StocktakeQueueItem,
  draft: Omit<StocktakeQueueItem, "clientOperationId" | "countedAt" | "status" | "conflictReason">,
): boolean {
  return (
    item.status === "PENDING" &&
    item.actorId === draft.actorId &&
    item.organizationId === draft.organizationId &&
    item.storeId === draft.storeId &&
    item.productId === draft.productId &&
    item.batchId === draft.batchId &&
    item.expectedQuantity === draft.expectedQuantity &&
    item.actualQuantity === draft.actualQuantity &&
    item.note === draft.note
  );
}

export function evaluateStocktakeRetry(
  item: StocktakeQueueItem,
  context: StocktakeRetryContext,
): StocktakeRetryDecision {
  if (item.organizationId !== context.organizationId || item.storeId !== context.storeId) {
    return {
      allowed: false,
      reason:
        "Thao tác thuộc cửa hàng khác. Hãy đăng nhập đúng cửa hàng đã tạo thao tác để đồng bộ.",
    };
  }
  if (item.actorId !== context.actorId) {
    return {
      allowed: false,
      reason:
        "Thao tác thuộc tài khoản khác. Hãy đăng nhập lại đúng tài khoản đã tạo thao tác để đồng bộ.",
    };
  }
  if (!context.canSync) {
    return {
      allowed: false,
      reason: "Phiên hiện tại không còn quyền gửi kiểm kê. Hãy liên hệ quản lý quyền.",
    };
  }
  if (item.status === "CONFLICT") {
    return {
      allowed: false,
      reason: item.conflictReason || "Tồn đã thay đổi. Hãy kiểm lại và tạo thao tác mới.",
    };
  }
  return { allowed: true };
}
