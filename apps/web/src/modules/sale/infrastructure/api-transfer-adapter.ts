import { SaleApiError, SaleNetworkError } from "../domain/sale";

export type TransferStatus = "PENDING" | "COMPLETED" | "CANCELLED" | "EXPIRED";
export interface TransferOrder {
  id: string;
  invoiceId: string;
  ownerId: string;
  reference: string;
  amount: number;
  status: TransferStatus;
  bankBin: string;
  accountNumber: string;
  accountName: string;
  createdAt: string;
  expiresAt: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
  bankReference: string | null;
  resolutionNote: string | null;
  qrPayload: string | null;
  confirmationMethod?: "EMPLOYEE" | "BANK_RECEIPT" | null;
}
export interface TransferConfig {
  enabled: boolean;
  bankBin: string | null;
  accountNumber: string | null;
  accountName: string | null;
  expiresInMinutes: number;
}
export interface TransferCreation {
  operationId: string;
  cartVersion: number;
  expectedTotal: number;
}
export interface TransferHistory {
  items: TransferOrder[];
  summary: {
    pending: number;
    completed: number;
    cancelled: number;
    expired: number;
    receivedAmount: number;
  };
  page: number;
  size: number;
}
export type TransferFetch = (path: string, init?: RequestInit) => Promise<Response>;

export class ApiTransferAdapter {
  constructor(private readonly fetchApi: TransferFetch) {}
  private async request<T>(path: string, body?: unknown): Promise<T> {
    let response: Response;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      response = await this.fetchApi("/api/v1/sales/transfers" + path, {
        cache: "no-store",
        signal: controller.signal,
        ...(body === undefined
          ? {}
          : {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify(body),
            }),
      });
    } catch (error) {
      throw new SaleNetworkError(error);
    } finally {
      clearTimeout(timeout);
    }
    if (!response.ok) {
      const error = (await response.json().catch(() => ({}))) as {
        code?: string;
        message?: string;
      };
      throw new SaleApiError(
        error.code ?? "TRANSFER_ERROR",
        error.message ?? "Chưa đọc được kết quả chuyển khoản.",
        response.status,
      );
    }
    return response.json() as Promise<T>;
  }
  config() {
    return this.request<TransferConfig>("/config");
  }
  history(page = 0) {
    return this.request<TransferHistory>("?page=" + page + "&size=10");
  }
  get(id: string) {
    return this.request<TransferOrder>("/" + encodeURIComponent(id));
  }
  create(operation: TransferCreation) {
    return this.request<TransferOrder>("", operation);
  }
  confirm(id: string, receivedAmount: number, bankReference: string, checkedBankReceipt: boolean) {
    return this.request<TransferOrder>("/" + encodeURIComponent(id) + "/confirm", {
      receivedAmount,
      bankReference,
      checkedBankReceipt,
    });
  }
  cancel(id: string, reason: string) {
    return this.request<TransferOrder>("/" + encodeURIComponent(id) + "/cancel", { reason });
  }
  confirmManual(id: string) {
    return this.request<TransferOrder>("/" + encodeURIComponent(id) + "/confirm-manual", {
      confirmed: true,
    });
  }
}
