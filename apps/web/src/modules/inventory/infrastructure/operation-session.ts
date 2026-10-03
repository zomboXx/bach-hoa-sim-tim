import type { PendingReceiptOperation } from "../domain/inventory";

const prefix = "simtim:inventory:receipt-operation:";

export function loadPendingOperation(userId: string): PendingReceiptOperation | undefined {
  try {
    const raw = sessionStorage.getItem(prefix + userId);
    if (!raw) return undefined;
    const value = JSON.parse(raw) as PendingReceiptOperation;
    if (!value || !value.idempotencyKey || !value.clientOperationId || !value.draft)
      return undefined;
    return value;
  } catch {
    return undefined;
  }
}

export function savePendingOperation(userId: string, operation: PendingReceiptOperation) {
  sessionStorage.setItem(prefix + userId, JSON.stringify(operation));
}

export function clearPendingOperation(userId: string) {
  sessionStorage.removeItem(prefix + userId);
}
