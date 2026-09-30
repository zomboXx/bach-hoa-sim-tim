package vn.simtim.api.sale.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.sale.domain.Invoice;
import vn.simtim.api.sale.domain.InvoiceLine;
import vn.simtim.api.sale.domain.Payment;

/** Response đầy đủ của một hoá đơn. */
public record InvoiceResponse(
        UUID id,
        UUID storeId,
        String invoiceNo,
        String status,
        UUID soldBy,
        Instant soldAt,
        long subtotal,
        long discountTotal,
        long grandTotal,
        long paidTotal,
        List<LineResponse> lines,
        List<PaymentResponse> payments) {

    public record LineResponse(
            UUID id,
            UUID productId,
            String skuSnapshot,
            String productNameSnapshot,
            BigDecimal quantity,
            long unitPrice,
            long discountAmount,
            long lineTotal) {}

    public record PaymentResponse(
            UUID id,
            String method,
            String status,
            long amount,
            Instant paidAt) {}

    public static InvoiceResponse from(Invoice inv) {
        var lines = inv.lines().stream().map(InvoiceResponse::lineFrom).toList();
        var payments = inv.payments().stream().map(InvoiceResponse::paymentFrom).toList();
        return new InvoiceResponse(inv.id(), inv.storeId(), inv.invoiceNo(), inv.status(),
                inv.soldBy(), inv.soldAt(), inv.subtotal(), inv.discountTotal(), inv.grandTotal(),
                inv.paidTotal(), lines, payments);
    }

    private static LineResponse lineFrom(InvoiceLine l) {
        return new LineResponse(l.id(), l.productId(), l.skuSnapshot(), l.productNameSnapshot(),
                l.quantity(), l.unitPrice(), l.discountAmount(), l.lineTotal());
    }

    private static PaymentResponse paymentFrom(Payment p) {
        return new PaymentResponse(p.id(), p.method(), p.status(), p.amount(), p.paidAt());
    }
}
