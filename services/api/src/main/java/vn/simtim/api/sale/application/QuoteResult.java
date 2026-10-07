package vn.simtim.api.sale.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Kết quả preview giá (quote) — không ghi DB, chỉ tính giá. */
public record QuoteResult(
        UUID storeId,
        List<QuoteLineResult> lines,
        long subtotal,
        long discountTotal,
        long grandTotal) {

    public record QuoteLineResult(
            UUID productId,
            String productName,
            UUID productBatchId,
            BigDecimal quantity,
            long unitPrice,
            long grossAmount,
            long discountAmount,
            long lineTotal,
            UUID appliedPromotionId,
            String promotionCode,
            String promotionName) {}
}
