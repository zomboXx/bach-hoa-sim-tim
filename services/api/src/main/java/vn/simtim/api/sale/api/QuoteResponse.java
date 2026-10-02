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
        long grandTotal) {

    public record LineResponse(
            UUID productId,
            String productName,
            BigDecimal quantity,
            long unitPrice,
            long lineTotal) {}

    public static QuoteResponse from(QuoteResult r) {
        var lines = r.lines().stream()
                .map(l -> new LineResponse(l.productId(), l.productName(),
                        l.quantity(), l.unitPrice(), l.lineTotal()))
                .toList();
        return new QuoteResponse(r.storeId(), lines, r.subtotal(), r.grandTotal());
    }
}
