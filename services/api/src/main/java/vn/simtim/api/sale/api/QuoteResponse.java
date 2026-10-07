package vn.simtim.api.sale.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.sale.application.QuoteResult;

/** Response cho endpoint /quote. */
public record QuoteResponse(
        UUID storeId,
        List<LineResponse> lines,
        long subtotal,
        long discountTotal,
        long grandTotal) {

    public record LineResponse(
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

    public static QuoteResponse from(QuoteResult r) {
        var lines = r.lines().stream()
                .map(l -> new LineResponse(l.productId(), l.productName(), l.productBatchId(),
                        l.quantity(), l.unitPrice(), l.grossAmount(), l.discountAmount(), l.lineTotal(),
                        l.appliedPromotionId(), l.promotionCode(), l.promotionName()))
                .toList();
        return new QuoteResponse(r.storeId(), lines, r.subtotal(), r.discountTotal(), r.grandTotal());
    }
}
