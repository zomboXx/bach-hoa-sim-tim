package vn.simtim.api.promotion.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.promotion.domain.Promotion;
import vn.simtim.api.promotion.domain.PromotionProduct;

/** Response body cho khuyến mãi (bao gồm danh sách productId trong phạm vi). */
public record PromotionResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String code,
        String name,
        String discountType,
        BigDecimal discountValue,
        Instant startsAt,
        Instant endsAt,
        String status,
        List<UUID> productIds) {

    public static PromotionResponse from(Promotion p, List<PromotionProduct> products) {
        return new PromotionResponse(
                p.id(), p.organizationId(), p.storeId(),
                p.code(), p.name(), p.discountType(), p.discountValue(),
                p.startsAt(), p.endsAt(), p.status(),
                products.stream().map(PromotionProduct::productId).toList());
    }

    /** Dùng khi không cần danh sách sản phẩm (list endpoint). */
    public static PromotionResponse from(Promotion p) {
        return from(p, List.of());
    }
}
