package vn.simtim.api.promotion.infrastructure;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Embeddable;

@Embeddable
class PromotionBatchId implements Serializable {
    UUID promotionId;
    UUID productBatchId;

    PromotionBatchId() {}

    PromotionBatchId(UUID promotionId, UUID productBatchId) {
        this.promotionId = promotionId;
        this.productBatchId = productBatchId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PromotionBatchId that)) return false;
        return Objects.equals(promotionId, that.promotionId)
                && Objects.equals(productBatchId, that.productBatchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promotionId, productBatchId);
    }
}
