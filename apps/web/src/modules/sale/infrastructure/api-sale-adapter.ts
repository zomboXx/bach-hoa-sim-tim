/**
 * ApiSaleAdapter — wires the SalePort to the real Spring Boot backend.
 *
 * Follows ApiAuthAdapter.fetchApi pattern already used by inventory and promotion modules.
 * - All requests carry Bearer token + X-Organization-Id via fetchApi.
 * - Network errors (TypeError) are wrapped in SaleNetworkError.
 * - Non-2xx responses are decoded and thrown as SaleApiError with the server `code`.
 * - Checkout sends optional Idempotency-Key header when provided (S2-GAP-04 contract).
 */

import {
  SaleApiError,
  SaleNetworkError,
  type ApiInvoice,
  type CartLine,
  type CheckoutRequest,
  type QuoteResult,
  type SalePort,
} from "../domain/sale";

type FetchFn = (path: string, init?: RequestInit) => Promise<Response>;

async function decodeError(res: Response, fallbackCode: string): Promise<never> {
  let code = fallbackCode;
  let message = `Lỗi máy chủ (${res.status})`;
  try {
    const body = (await res.json()) as Record<string, unknown>;
    if (typeof body.code === "string") code = body.code;
    if (typeof body.message === "string") message = body.message;
  } catch {
    // body not JSON, keep defaults
  }
  throw new SaleApiError(code, message, res.status);
}

export class ApiSaleAdapter implements SalePort {
  constructor(private readonly fetchApi: FetchFn) {}

  async quote(lines: CartLine[]): Promise<QuoteResult> {
    let res: Response;
    try {
      res = await this.fetchApi("/api/v1/sales/quote", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ items: lines }),
      });
    } catch (e) {
      throw new SaleNetworkError(e);
    }
    if (!res.ok) return decodeError(res, "QUOTE_FAILED");
    return res.json() as Promise<QuoteResult>;
  }

  async checkout(req: CheckoutRequest): Promise<ApiInvoice> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (req.idempotencyKey) {
      headers["Idempotency-Key"] = req.idempotencyKey;
    }
    let res: Response;
    try {
      res = await this.fetchApi("/api/v1/sales/checkout", {
        method: "POST",
        headers,
        body: JSON.stringify({
          items: req.items,
          expectedTotal: req.expectedTotal,
          cashAmount: req.cashAmount,
          ...(req.cartVersion === undefined ? {} : { cartVersion: req.cartVersion }),
        }),
      });
    } catch (e) {
      throw new SaleNetworkError(e);
    }
    if (!res.ok) return decodeError(res, "CHECKOUT_FAILED");
    return res.json() as Promise<ApiInvoice>;
  }

  async listInvoices(): Promise<ApiInvoice[]> {
    let res: Response;
    try {
      res = await this.fetchApi("/api/v1/sales/invoices");
    } catch (e) {
      throw new SaleNetworkError(e);
    }
    if (!res.ok) return decodeError(res, "LIST_INVOICES_FAILED");
    return res.json() as Promise<ApiInvoice[]>;
  }

  async getInvoice(id: string): Promise<ApiInvoice> {
    let res: Response;
    try {
      res = await this.fetchApi(`/api/v1/sales/invoices/${encodeURIComponent(id)}`);
    } catch (e) {
      throw new SaleNetworkError(e);
    }
    if (!res.ok) return decodeError(res, "GET_INVOICE_FAILED");
    return res.json() as Promise<ApiInvoice>;
  }
}
