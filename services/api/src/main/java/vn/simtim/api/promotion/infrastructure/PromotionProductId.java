package vn.simtim.api.promotion.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Khóa ghép cho sales.promotion_products (promotion_id, product_id). */
@Embeddable
class PromotionProductId implements Serializable {

    @Column(name = "promotion_id", nullable = false)
    UUID promotionId;

    @Column(name = "product_id", nullable = false)
    UUID productId;

    PromotionProductId() {}

    PromotionProductId(UUID promotionId, UUID productId) {
        this.promotionId = promotionId;
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromotionProductId other)) return false;
        return Objects.equals(promotionId, other.promotionId) && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promotionId, productId);
    }
}
