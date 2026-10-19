/**
 * DemoSaleAdapter — wraps the existing synchronous api.ts checkout/invoice logic
 * behind the SalePort interface so App.vue can use the same code path in demo mode.
 *
 * This adapter does NOT call any HTTP endpoint. All data lives in IndexedDB via
 * the existing `persist`/`readState` helpers.
 */

import {
  checkout as demoCheckout,
  price as demoPrice,
  today,
  type State,
  type User,
} from "../../../api";
import {
  SaleApiError,
  type ApiInvoice,
  type CartLine,
  type CheckoutRequest,
  type QuoteResult,
  type SalePort,
} from "../domain/sale";

export class DemoSaleAdapter implements SalePort {
  constructor(
    private readonly getState: () => State,
    private readonly persistState: (next: State) => Promise<void>,
    private readonly getUser: () => User,
  ) {}

  async quote(lines: CartLine[]): Promise<QuoteResult> {
    const s = this.getState();
    let subtotal = 0;
    const resultLines: QuoteResult["lines"] = [];
    for (const line of lines) {
      const product = s.products.find((p) => p.id === line.productId);
      if (!product)
        throw new SaleApiError("INVALID_REQUEST", `Sản phẩm không tồn tại: ${line.productId}`, 422);
      const qty = parseFloat(line.quantity);
      const unitPrice = demoPrice(s, product);
      const gross = Math.round(unitPrice * qty);
      const discount =
        product.price > unitPrice ? Math.round((product.price - unitPrice) * qty) : 0;
      subtotal += gross - discount;
      resultLines.push({
        productId: product.id,
        productName: product.name,
        quantity: line.quantity,
        unitPrice,
        grossAmount: gross,
        discountAmount: discount,
        lineTotal: gross - discount,
      });
    }
    return {
      storeId: "DEMO",
      lines: resultLines,
      subtotal,
      discountTotal: resultLines.reduce((n, l) => n + l.discountAmount, 0),
      grandTotal: subtotal,
    };
  }

  async checkout(req: CheckoutRequest): Promise<ApiInvoice> {
    const s = JSON.parse(JSON.stringify(this.getState())) as State;
    const user = this.getUser();
    // Verify expectedTotal matches current quote
    const quoteResult = await this.quote(req.items);
    if (quoteResult.grandTotal !== req.expectedTotal) {
      throw new SaleApiError(
        "PRICE_CHANGED",
        `Giá sản phẩm đã thay đổi. Tổng mới: ${quoteResult.grandTotal}, tổng kỳ vọng: ${req.expectedTotal}`,
        409,
      );
    }
    const lines = req.items.map((l) => ({ id: l.productId, quantity: parseInt(l.quantity, 10) }));
    const invoice = demoCheckout(s, user, lines, "Tiền mặt");
    await this.persistState(s);
    return {
      id: invoice.id,
      invoiceNo: invoice.id,
      status: "COMPLETED",
      soldAt: invoice.at,
      soldBy: user.name,
      subtotal: invoice.total,
      discountTotal: 0,
      grandTotal: invoice.total,
      paidTotal: invoice.total,
      changeAmount: req.cashAmount - invoice.total,
      lines: invoice.lines.map((l) => ({
        productId: s.products.find((p) => p.name === l.name)?.id ?? l.name,
        sku: l.name,
        productName: l.name,
        quantity: String(l.quantity),
        unitPrice: l.price,
        discountAmount: 0,
        lineTotal: l.price * l.quantity,
      })),
    };
  }

  async listInvoices(): Promise<ApiInvoice[]> {
    const s = this.getState();
    return s.invoices.map((inv) => ({
      id: inv.id,
      invoiceNo: inv.id,
      status: "COMPLETED",
      soldAt: inv.at,
      soldBy: inv.staff,
      subtotal: inv.total,
      discountTotal: 0,
      grandTotal: inv.total,
      paidTotal: inv.total,
      changeAmount: 0,
      lines: inv.lines.map((l) => ({
        productId: l.name,
        sku: l.name,
        productName: l.name,
        quantity: String(l.quantity),
        unitPrice: l.price,
        discountAmount: 0,
        lineTotal: l.price * l.quantity,
      })),
    }));
  }

  async getInvoice(id: string): Promise<ApiInvoice> {
    const invoices = await this.listInvoices();
    const found = invoices.find((inv) => inv.id === id);
    if (!found) throw new SaleApiError("NOT_FOUND", `Hóa đơn không tồn tại: ${id}`, 404);
    return found;
  }
}

// Re-export today for App.vue usage
export { today };
