import {
  InventoryApiError,
  type CatalogProduct,
  type CatalogSupplier,
  type ConfirmReceiptCommand,
  type InventoryPort,
  type InventorySnapshot,
  type Receipt,
} from "../domain/inventory";

type AuthorizedFetch = (path: string, init?: RequestInit) => Promise<Response>;

interface InventoryPage<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export class ApiInventoryAdapter implements InventoryPort {
  readonly mode = "api" as const;

  constructor(
    private readonly authorizedFetch: AuthorizedFetch,
    private readonly timeoutMs = Number(import.meta.env.VITE_INVENTORY_TIMEOUT_MS || 15000),
  ) {}

  async loadCatalog() {
    const [suppliers, products] = await Promise.all([
      this.get<CatalogSupplier[]>("/api/v1/suppliers"),
      this.get<CatalogProduct[]>("/api/v1/products"),
    ]);
    return {
      suppliers: suppliers.filter((item) => item.status === "ACTIVE"),
      products: products.filter((item) => item.status === "ACTIVE"),
    };
  }

  async listReceipts() {
    return this.get<Receipt[]>("/api/v1/inventory/receipts?limit=20&offset=0");
  }

  async findReceiptByClientOperationId(clientOperationId: string) {
    const receipts = await this.get<Receipt[]>(
      `/api/v1/inventory/receipts?clientOperationId=${encodeURIComponent(clientOperationId)}`,
    );
    return receipts[0];
  }

  async confirmReceipt(command: ConfirmReceiptCommand) {
    const { draft } = command;
    return this.request<Receipt>("/api/v1/inventory/receipts", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": command.idempotencyKey,
      },
      body: JSON.stringify({
        supplierId: draft.supplierId,
        clientOperationId: command.clientOperationId,
        lines: [
          {
            productId: draft.productId,
            expectedQuantity: draft.expectedQuantity,
            deliveredQuantity: draft.deliveredQuantity,
            acceptedQuantity: draft.acceptedQuantity,
            rejectedQuantity: draft.rejectedQuantity,
            unitCost: Number(draft.unitCost),
            supplierLotNumber: draft.supplierLotNumber.trim() || null,
            expiryDate: draft.expiryDate || null,
            discrepancyReason: draft.discrepancyReason.trim() || null,
          },
        ],
      }),
    });
  }

  async loadInventory(): Promise<InventorySnapshot> {
    const [products, batches, movements] = await Promise.all([
      this.getAllPages<InventorySnapshot["products"][number]>("/api/v1/inventory/products"),
      this.getAllPages<InventorySnapshot["batches"][number]>("/api/v1/inventory/batches"),
      this.getAllPages<InventorySnapshot["movements"][number]>("/api/v1/inventory/movements"),
    ]);
    return { products, batches, movements };
  }

  private async getAllPages<T>(path: string) {
    const items: T[] = [];
    const size = 100;
    for (let page = 0; page < 10000; page++) {
      const result = await this.get<InventoryPage<T>>(`${path}?page=${page}&size=${size}`);
      items.push(...result.items);
      if (page + 1 >= result.totalPages || result.items.length === 0) return items;
    }
    throw new InventoryApiError("Dữ liệu tồn kho vượt quá giới hạn phân trang an toàn.");
  }

  private get<T>(path: string) {
    return this.request<T>(path, { method: "GET" });
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const response = await this.authorizedFetch(path, {
        ...init,
        cache: "no-store",
        credentials: "omit",
        signal: controller.signal,
      });
      if (!response.ok) throw await apiError(response);
      return (await response.json()) as T;
    } catch (error) {
      if (error instanceof InventoryApiError) throw error;
      if (error instanceof DOMException && error.name === "AbortError")
        throw new InventoryApiError(
          "Máy chủ chưa phản hồi. Hãy kiểm tra trạng thái thao tác trước khi thử lại.",
          undefined,
          {},
          true,
        );
      throw new InventoryApiError("Không thể kết nối máy chủ. Không có dữ liệu demo thay thế.");
    } finally {
      window.clearTimeout(timer);
    }
  }
}

async function apiError(response: Response): Promise<InventoryApiError> {
  let body: Record<string, unknown> = {};
  try {
    body = (await response.json()) as Record<string, unknown>;
  } catch {
    // Status still provides a safe fallback message.
  }
  const fieldErrors: Record<string, string> = {};
  if (Array.isArray(body.fieldErrors)) {
    for (const item of body.fieldErrors) {
      if (item && typeof item === "object") {
        const entry = item as Record<string, unknown>;
        if (typeof entry.field === "string" && typeof entry.message === "string")
          fieldErrors[entry.field.replace(/^lines\[0\]\./, "")] = entry.message;
      }
    }
  } else if (body.fieldErrors && typeof body.fieldErrors === "object") {
    for (const [field, message] of Object.entries(body.fieldErrors as Record<string, unknown>)) {
      if (typeof message === "string") fieldErrors[field.replace(/^lines\[0\]\./, "")] = message;
    }
  }
  const fallback =
    response.status === 401
      ? "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại."
      : response.status === 403
        ? "Bạn không có quyền thực hiện thao tác này."
        : `Máy chủ từ chối yêu cầu (${response.status}).`;
  return new InventoryApiError(
    typeof body.message === "string" ? body.message : fallback,
    response.status,
    fieldErrors,
  );
}
