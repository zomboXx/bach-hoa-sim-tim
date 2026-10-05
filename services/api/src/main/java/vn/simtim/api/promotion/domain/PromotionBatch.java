package vn.simtim.api.promotion.domain;

import java.util.UUID;

/** Liên kết một lô hàng vào phạm vi khuyến mãi. */
public record PromotionBatch(
        UUID organizationId,
        UUID promotionId,
        UUID productBatchId) {}
