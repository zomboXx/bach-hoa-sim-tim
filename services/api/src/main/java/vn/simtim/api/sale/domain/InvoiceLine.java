package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Dòng hóa đơn: snapshot SKU/tên sản phẩm và giá tại thời điểm bán. */
public record InvoiceLine(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID invoiceId,
        UUID productId,
        String skuSnapshot,
        String productNameSnapshot,
        BigDecimal quantity,
        long unitPrice,
        long discountAmount,
        long lineTotal,
        UUID appliedPromotionId) {}
