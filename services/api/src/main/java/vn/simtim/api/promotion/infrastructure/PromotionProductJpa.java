package vn.simtim.api.promotion.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;
import vn.simtim.api.promotion.domain.PromotionProduct;

@Entity
@Table(schema = "sales", name = "promotion_products")
class PromotionProductJpa {

    @EmbeddedId
    PromotionProductId id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    PromotionProductJpa() {}

    PromotionProductJpa(PromotionProduct pp) {
        this.id = new PromotionProductId(pp.promotionId(), pp.productId());
        this.organizationId = pp.organizationId();
    }

    PromotionProduct toDomain() {
        return new PromotionProduct(organizationId, id.promotionId, id.productId);
    }
}
