import type { StocktakeQueueItem, StocktakeStatus } from "../domain/stocktake";

type AuthorizedFetch = (path: string, init?: RequestInit) => Promise<Response>;

export interface ApiStocktakeLine {
  id: string;
  stocktakeId: string;
  productId: string;
  batchId: string;
  clientOperationId: string;
  expectedQuantity: number;
  actualQuantity: number;
  baseVersion: number;
  note: string;
  status: Exclude<StocktakeStatus, "REVIEW">;
  conflictReason?: string | null;
  countedAt: string;
}

export interface ApiStocktakeSession {
  id: string;
  status: string;
  lines: ApiStocktakeLine[];
}

export class StocktakeApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string,
  ) {
    super(message);
  }
}

export class ApiStocktakeAdapter {
  constructor(private readonly authorizedFetch: AuthorizedFetch) {}

  openSession() {
    return this.request<ApiStocktakeSession>("/api/v1/inventory/stocktakes", {
      method: "POST",
    });
  }

  getSession(sessionId: string) {
    return this.request<ApiStocktakeSession>(
      `/api/v1/inventory/stocktakes/${encodeURIComponent(sessionId)}`,
      { method: "GET" },
    );
  }

  approveSession(sessionId: string) {
    return this.request<ApiStocktakeSession>(
      `/api/v1/inventory/stocktakes/${encodeURIComponent(sessionId)}/approve`,
      { method: "POST" },
    );
  }

  submitCount(sessionId: string, operation: StocktakeQueueItem) {
    return this.request<ApiStocktakeLine>(
      `/api/v1/inventory/stocktakes/${encodeURIComponent(sessionId)}/counts`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          clientOperationId: operation.clientOperationId,
          batchId: operation.batchId,
          actualQuantity: operation.actualQuantity,
          baseVersion: operation.baseVersion,
          note: operation.note,
          countedAt: operation.countedAt,
        }),
      },
    );
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    let response: Response;
    try {
      response = await this.authorizedFetch(path, {
        ...init,
        cache: "no-store",
        credentials: "omit",
      });
    } catch {
      throw new StocktakeApiError("Không thể kết nối máy chủ. Phiếu vẫn được giữ trên thiết bị.");
    }
    if (!response.ok) {
      let body: Record<string, unknown> = {};
      try {
        body = (await response.json()) as Record<string, unknown>;
      } catch {
        // HTTP status remains usable when an intermediary returned a non-JSON body.
      }
      throw new StocktakeApiError(
        typeof body.message === "string"
          ? body.message
          : `Máy chủ từ chối phiếu kiểm kê (${response.status}).`,
        response.status,
        typeof body.code === "string" ? body.code : undefined,
      );
    }
    return (await response.json()) as T;
  }
}
