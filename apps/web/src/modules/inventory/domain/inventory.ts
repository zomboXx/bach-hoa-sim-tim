import type { State } from "../../../api";

export type InventoryMode = "demo" | "api";

export interface CatalogSupplier {
  id: string;
  code: string;
  name: string;
  status: "ACTIVE" | "INACTIVE";
}

export interface CatalogProduct {
  id: string;
  sku: string;
  name: string;
  tracksExpiry: boolean;
  status: "ACTIVE" | "INACTIVE";
}

export interface ReceiptDraft {
  supplierId: string;
  productId: string;
  expectedQuantity: string;
  deliveredQuantity: string;
  acceptedQuantity: string;
  rejectedQuantity: string;
  unitCost: string;
  supplierLotNumber: string;
  expiryDate: string;
  discrepancyReason: string;
}

export interface ReceiptLine extends Omit<ReceiptDraft, "supplierId" | "unitCost"> {
  id: string;
  unitCost: number;
}

export interface Receipt {
  id: string;
  supplierId: string;
  status: string;
  receivedAt: string;
  clientOperationId: string;
  lines: ReceiptLine[];
}

export interface InventoryProduct {
  productId: string;
  sku: string;
  productName: string;
  unitCode?: string;
  productStatus: "ACTIVE" | "INACTIVE";
  onHandQuantity: string;
  availableQuantity: string;
  businessDate: string;
}

export type BatchStatus = "AVAILABLE" | "BLOCKED" | "EXHAUSTED";
export type ExpiryStatus = "EXPIRED" | "NEAR_EXPIRY" | "VALID" | "NO_EXPIRY";

export interface InventoryBatch {
  batchId: string;
  productId: string;
  batchNumber: string;
  supplierLotNumber: string | null;
  status: BatchStatus;
  expiryStatus: ExpiryStatus;
  expiryDate: string | null;
  receivedDate: string;
  onHandQuantity: string;
  availableQuantity: string;
  businessDate: string;
}

export interface InventoryMovement {
  movementId: string;
  productId: string;
  batchId: string;
  type: "RECEIPT" | "SALE" | "ADJUSTMENT";
  quantityDelta: string;
  source: { type: "GOODS_RECEIPT" | "INVOICE" | "ADJUSTMENT"; id: string };
  occurredAt: string;
  recordedBy: string;
}

export interface InventorySnapshot {
  products: InventoryProduct[];
  batches: InventoryBatch[];
  movements: InventoryMovement[];
}

export interface ConfirmReceiptCommand {
  idempotencyKey: string;
  clientOperationId: string;
  draft: ReceiptDraft;
}

export interface InventoryPort {
  readonly mode: InventoryMode;
  loadCatalog(): Promise<{ suppliers: CatalogSupplier[]; products: CatalogProduct[] }>;
  listReceipts(): Promise<Receipt[]>;
  findReceiptByClientOperationId(clientOperationId: string): Promise<Receipt | undefined>;
  confirmReceipt(command: ConfirmReceiptCommand): Promise<Receipt>;
  loadInventory(): Promise<InventorySnapshot>;
}

export interface PendingReceiptOperation extends ConfirmReceiptCommand {
  state: "posting" | "uncertain";
}

export type DemoStateReader = () => State;
export type DemoStateWriter = (state: State) => Promise<void>;

export class InventoryApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly fieldErrors: Record<string, string> = {},
    readonly timedOut = false,
  ) {
    super(message);
  }
}
