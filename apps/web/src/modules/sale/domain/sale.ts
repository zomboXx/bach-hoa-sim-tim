/**
 * Sale domain — public types and port for S2-GAP-05.
 *
 * Naming follows the Sprint 2 wire contract in contracts/SPRINT_2_BOUNDARY_DRAFT.md.
 * All monetary values are integer VND.  Quantities are decimal strings on the wire.
 */

export interface SaleProduct {
  /** Server product UUID */
  id: string;
  sku: string;
  name: string;
  /** Price in VND integer, valid at quote/checkout time */
  unitPrice: number;
  /** Available quantity (decimal string from server) */
  availableQuantity: string;
  unit: string;
}

export interface QuoteLineResult {
  productId: string;
  productName: string;
  quantity: string;
  unitPrice: number;
  grossAmount: number;
  discountAmount: number;
  lineTotal: number;
}

export interface QuoteResult {
  storeId: string;
  lines: QuoteLineResult[];
  subtotal: number;
  discountTotal: number;
  grandTotal: number;
}

export interface CartLine {
  productId: string;
  /** Decimal string, e.g. "1.000" */
  quantity: string;
}

export interface CheckoutRequest {
  items: CartLine[];
  expectedTotal: number;
  cashAmount: number;
  /** Optional idempotency UUID for safe retry */
  idempotencyKey?: string;
}

export interface InvoiceLine {
  productId: string;
  sku: string;
  productName: string;
  quantity: string;
  unitPrice: number;
  discountAmount: number;
  lineTotal: number;
}

export interface ApiInvoice {
  id: string;
  invoiceNo: string;
  status: string;
  soldAt: string;
  soldBy: string;
  subtotal: number;
  discountTotal: number;
  grandTotal: number;
  paidTotal: number;
  changeAmount: number;
  lines: InvoiceLine[];
}

/** Error codes returned by the server; used for UI-specific messaging. */
export type SaleErrorCode =
  | "PRICE_CHANGED"
  | "INSUFFICIENT_STOCK"
  | "IDEMPOTENCY_KEY_REUSED"
  | "FORBIDDEN"
  | "UNAUTHENTICATED"
  | "INVALID_REQUEST"
  | "TEMPORARILY_UNAVAILABLE"
  | string;

export class SaleApiError extends Error {
  constructor(
    public readonly code: SaleErrorCode,
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "SaleApiError";
  }
}

export class SaleNetworkError extends Error {
  constructor(cause?: unknown) {
    super("Lỗi mạng: Không thể kết nối đến máy chủ.");
    this.name = "SaleNetworkError";
    if (cause) this.cause = cause;
  }
}

/** Port consumed by App.vue. Demo and API implementations share this shape. */
export interface SalePort {
  /**
   * Preview cart totals, promotions and FEFO allocations without reserving stock.
   * @throws SaleApiError | SaleNetworkError
   */
  quote(lines: CartLine[]): Promise<QuoteResult>;

  /**
   * Create invoice atomically: FEFO allocation + stock deduction + payment.
   * Returns the created invoice on success (HTTP 201) or the cached invoice on
   * idempotency replay (HTTP 200/201 per server).
   * @throws SaleApiError(PRICE_CHANGED) | SaleApiError(INSUFFICIENT_STOCK) | SaleNetworkError
   */
  checkout(req: CheckoutRequest): Promise<ApiInvoice>;

  /**
   * List invoices for the current store (scoped by session user role).
   * SALES sees own invoices only; MANAGER/ADMIN see entire store.
   * @throws SaleApiError | SaleNetworkError
   */
  listInvoices(): Promise<ApiInvoice[]>;

  /**
   * Fetch a single invoice by ID (scoped check applied server-side).
   * @throws SaleApiError(NOT_FOUND) | SaleNetworkError
   */
  getInvoice(id: string): Promise<ApiInvoice>;
}
