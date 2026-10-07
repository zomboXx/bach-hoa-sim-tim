package vn.simtim.api.promotion.domain;

import java.util.UUID;

/** Liên kết sản phẩm vào phạm vi khuyến mãi. */
public record PromotionProduct(
        UUID organizationId,
        UUID promotionId,
        UUID productId) {}
