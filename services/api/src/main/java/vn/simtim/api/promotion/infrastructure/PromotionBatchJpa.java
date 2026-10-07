package vn.simtim.api.promotion.infrastructure;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import vn.simtim.api.promotion.domain.PromotionBatch;

@Entity
@Table(schema = "sales", name = "promotion_batches")
class PromotionBatchJpa {

    @EmbeddedId
    PromotionBatchId id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    PromotionBatchJpa() {}

    PromotionBatchJpa(PromotionBatch batch) {
        this.id = new PromotionBatchId(batch.promotionId(), batch.productBatchId());
        this.organizationId = batch.organizationId();
    }

    PromotionBatch toDomain() {
        return new PromotionBatch(organizationId, id.promotionId, id.productBatchId);
    }
}
