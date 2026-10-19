package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Hóa đơn bán hàng đã hoàn tất. Sau khi COMPLETED, dữ liệu snapshot bất biến. */
public record Invoice(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String invoiceNo,
        String status,
        UUID soldBy,
        Instant soldAt,
        long subtotal,
        long discountTotal,
        long grandTotal,
        long paidTotal,
        long changeAmount,
        List<InvoiceLine> lines,
        List<Payment> payments,
        long version,
        /** Idempotency key ghi từ header Idempotency-Key, null nếu không truyền. */
        UUID idempotencyKey) {

    /** Constructor tương thích ngược: không có idempotencyKey. */
    public Invoice(UUID id, UUID organizationId, UUID storeId, String invoiceNo, String status,
                   UUID soldBy, Instant soldAt, long subtotal, long discountTotal,
                   long grandTotal, long paidTotal, long changeAmount,
                   List<InvoiceLine> lines, List<Payment> payments, long version) {
        this(id, organizationId, storeId, invoiceNo, status, soldBy, soldAt,
                subtotal, discountTotal, grandTotal, paidTotal, changeAmount,
                lines, payments, version, null);
    }

    public boolean isCompleted() { return "COMPLETED".equals(status); }
}
