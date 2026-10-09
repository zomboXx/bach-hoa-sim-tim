import { localDay, today, uid, type State } from "../../../api";
import type {
  ConfirmReceiptCommand,
  DemoStateReader,
  DemoStateWriter,
  ExpiryStatus,
  InventoryBatch,
  InventoryPort,
  InventoryProduct,
  InventorySnapshot,
  Receipt,
} from "../domain/inventory";

export class DemoInventoryAdapter implements InventoryPort {
  readonly mode = "demo" as const;

  constructor(
    private readonly readState: DemoStateReader,
    private readonly writeState: DemoStateWriter,
  ) {}

  async loadCatalog() {
    const state = this.readState();
    return {
      suppliers: state.suppliers.map((supplier) => ({
        id: supplier.id,
        code: supplier.id,
        name: supplier.name,
        status: "ACTIVE" as const,
      })),
      products: state.products.map((product) => ({
        id: product.id,
        sku: product.id,
        name: product.name,
        tracksExpiry: true,
        status: "ACTIVE" as const,
      })),
    };
  }

  async listReceipts() {
    return this.readState().receipts.map(toReceipt);
  }

  async findReceiptByClientOperationId(clientOperationId: string) {
    return (await this.listReceipts()).find(
      (receipt) => receipt.clientOperationId === clientOperationId,
    );
  }

  async confirmReceipt(command: ConfirmReceiptCommand) {
    const current = this.readState();
    const state = JSON.parse(JSON.stringify(current)) as State;
    const existing = state.receipts.find(
      (receipt) => receipt.clientOperationId === command.clientOperationId,
    );
    if (existing) return toReceipt(existing);
    const { draft } = command;
    if (state.batches.some((batch) => batch.id === draft.supplierLotNumber.trim()))
      throw new Error("Mã lô đã tồn tại trong dữ liệu demo.");
    const receiptId = uid("NH");
    const receivedAt = new Date().toISOString();
    const accepted = Number(draft.acceptedQuantity);
    if (accepted > 0) {
      state.batches.push({
        id: draft.supplierLotNumber.trim() || uid("LO"),
        productId: draft.productId,
        quantity: accepted,
        expiry: draft.expiryDate || "9999-12-31",
      });
      state.movements.unshift({
        id: uid("BD"),
        at: receivedAt,
        productId: draft.productId,
        quantity: accepted,
        kind: "Nhận hàng",
        reference: receiptId,
      });
    }
    state.receipts.unshift({
      id: receiptId,
      supplier: draft.supplierId,
      product: draft.productId,
      expected: draft.expectedQuantity,
      delivered: draft.deliveredQuantity,
      accepted,
      rejected: Number(draft.rejectedQuantity),
      unitCost: Number(draft.unitCost),
      lot: draft.supplierLotNumber,
      expiry: draft.expiryDate,
      note: draft.discrepancyReason,
      receivedAt,
      clientOperationId: command.clientOperationId,
    });
    await this.writeState(state);
    return toReceipt(state.receipts[0]);
  }

  async loadInventory(): Promise<InventorySnapshot> {
    const state = this.readState();
    const products: InventoryProduct[] = state.products.map((product) => {
      const batches = state.batches.filter((batch) => batch.productId === product.id);
      return {
        productId: product.id,
        sku: product.id,
        productName: product.name,
        unitCode: product.unit,
        productStatus: "ACTIVE",
        onHandQuantity: quantity(batches.reduce((sum, batch) => sum + batch.quantity, 0)),
        availableQuantity: quantity(
          batches
            .filter((batch) => batch.expiry >= today())
            .reduce((sum, batch) => sum + batch.quantity, 0),
        ),
        businessDate: today(),
      };
    });
    const batches: InventoryBatch[] = state.batches.map((batch) => {
      const expiryStatus = classifyExpiry(batch.expiry);
      return {
        batchId: batch.id,
        productId: batch.productId,
        batchNumber: batch.id,
        supplierLotNumber: batch.id,
        status: batch.quantity <= 0 ? "EXHAUSTED" : "AVAILABLE",
        expiryStatus,
        expiryDate: batch.expiry === "9999-12-31" ? null : batch.expiry,
        receivedDate: today(),
        onHandQuantity: quantity(batch.quantity),
        availableQuantity: quantity(expiryStatus === "EXPIRED" ? 0 : batch.quantity),
        businessDate: today(),
      };
    });
    return {
      products,
      batches,
      movements: state.movements.map((movement) => {
        const isReceipt = movement.kind === "Nhận hàng";
        const isSale = movement.kind === "Bán hàng";
        return {
          movementId: movement.id,
          productId: movement.productId,
          batchId:
            state.batches.find((batch) => batch.productId === movement.productId)?.id || "demo",
          type: isReceipt ? "RECEIPT" : isSale ? "SALE" : "ADJUSTMENT",
          quantityDelta: quantity(movement.quantity),
          source: {
            type: isReceipt ? "GOODS_RECEIPT" : isSale ? "INVOICE" : "ADJUSTMENT",
            id: movement.reference,
          },
          occurredAt: movement.at,
          recordedBy: "demo",
        };
      }),
    };
  }
}

function toReceipt(receipt: State["receipts"][number]): Receipt {
  return {
    id: receipt.id,
    supplierId: receipt.supplier,
    status: "CONFIRMED",
    receivedAt: receipt.receivedAt || new Date().toISOString(),
    clientOperationId: receipt.clientOperationId || receipt.id,
    lines: [
      {
        id: `${receipt.id}-line`,
        productId: receipt.product,
        expectedQuantity: receipt.expected || String(receipt.accepted + receipt.rejected),
        deliveredQuantity: receipt.delivered || String(receipt.accepted + receipt.rejected),
        acceptedQuantity: String(receipt.accepted),
        rejectedQuantity: String(receipt.rejected),
        unitCost: receipt.unitCost || 0,
        supplierLotNumber: receipt.lot || "",
        expiryDate: receipt.expiry || "",
        discrepancyReason: receipt.note,
      },
    ],
  };
}

function quantity(value: number) {
  return value.toFixed(3).replace(/\.000$/, "");
}

function classifyExpiry(expiry: string): ExpiryStatus {
  if (expiry === "9999-12-31") return "NO_EXPIRY";
  const businessDate = localDay(new Date());
  if (expiry < businessDate) return "EXPIRED";
  const near = new Date(`${businessDate}T00:00:00+07:00`);
  near.setDate(near.getDate() + 7);
  return expiry <= localDay(near) ? "NEAR_EXPIRY" : "VALID";
}
