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
        long version) {

    public boolean isCompleted() { return "COMPLETED".equals(status); }
}
