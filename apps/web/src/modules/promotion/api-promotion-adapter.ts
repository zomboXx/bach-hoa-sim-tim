import type { CatalogProduct, InventoryBatch } from "../inventory/domain/inventory";

export interface Promotion {
  id: string;
  code: string;
  name: string;
  discountType: "PERCENT" | "AMOUNT";
  discountValue: number;
  startsAt: string;
  endsAt: string;
  status: "DRAFT" | "ACTIVE" | "INACTIVE";
  productIds: string[];
  batchIds: string[];
}

export type PromotionDraft = Omit<Promotion, "id" | "productIds" | "batchIds">;
type AuthorizedFetch = (path: string, init?: RequestInit) => Promise<Response>;
type Page<T> = { items: T[]; totalPages: number };

export class ApiPromotionAdapter {
  constructor(private readonly authorizedFetch: AuthorizedFetch) {}

  list() {
    return this.request<Promotion[]>("/api/v1/sales/promotions");
  }

  get(id: string) {
    return this.request<Promotion>(`/api/v1/sales/promotions/${encodeURIComponent(id)}`);
  }

  create(draft: PromotionDraft) {
    return this.request<Promotion>("/api/v1/sales/promotions", {
      method: "POST",
      body: JSON.stringify(draft),
    });
  }

  update(id: string, draft: PromotionDraft) {
    return this.request<Promotion>(`/api/v1/sales/promotions/${encodeURIComponent(id)}`, {
      method: "PUT",
      body: JSON.stringify(draft),
    });
  }

  async addTarget(id: string, kind: "PRODUCT" | "BATCH", targetId: string) {
    const path = kind === "PRODUCT" ? "products" : "batches";
    const body = kind === "PRODUCT" ? { productId: targetId } : { productBatchId: targetId };
    await this.request<void>(`/api/v1/sales/promotions/${encodeURIComponent(id)}/${path}`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  }

  async removeTarget(id: string, kind: "PRODUCT" | "BATCH", targetId: string) {
    const path = kind === "PRODUCT" ? "products" : "batches";
    await this.request<void>(
      `/api/v1/sales/promotions/${encodeURIComponent(id)}/${path}/${encodeURIComponent(targetId)}`,
      { method: "DELETE" },
    );
  }

  async loadChoices() {
    const products = await this.request<CatalogProduct[]>("/api/v1/products");
    const batches: InventoryBatch[] = [];
    for (let page = 0; page < 10000; page++) {
      const result = await this.request<Page<InventoryBatch>>(
        `/api/v1/inventory/batches?page=${page}&size=100`,
      );
      batches.push(...result.items);
      if (page + 1 >= result.totalPages || result.items.length === 0) break;
    }
    return { products, batches };
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const response = await this.authorizedFetch(path, {
      ...init,
      cache: "no-store",
      credentials: "omit",
      headers: {
        ...(init.headers as Record<string, string> | undefined),
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) {
      let detail = `Yêu cầu không thành công (${response.status}).`;
      try {
        const body = (await response.json()) as { detail?: string; message?: string };
        detail = body.detail || body.message || detail;
      } catch {
        // Keep the HTTP status when the response has no JSON body.
      }
      throw new Error(detail);
    }
    const raw = await response.text();
    return (raw ? JSON.parse(raw) : undefined) as T;
  }
}
