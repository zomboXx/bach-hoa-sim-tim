package vn.simtim.api.promotion.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.simtim.api.promotion.domain.Promotion;

@Entity
@Table(schema = "sales", name = "promotions")
class PromotionJpa {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "store_id")
    UUID storeId;

    @Column(nullable = false, length = 60)
    String code;

    @Column(nullable = false, length = 200)
    String name;

    @Column(name = "discount_type", nullable = false, length = 16)
    String discountType;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    BigDecimal discountValue;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    Instant startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "timestamptz")
    Instant endsAt;

    @Column(nullable = false, length = 16)
    String status;

    PromotionJpa() {}

    PromotionJpa(Promotion p) {
        this.id = p.id();
        this.organizationId = p.organizationId();
        this.storeId = p.storeId();
        this.code = p.code();
        this.name = p.name();
        this.discountType = p.discountType();
        this.discountValue = p.discountValue();
        this.startsAt = p.startsAt();
        this.endsAt = p.endsAt();
        this.status = p.status();
    }

    Promotion toDomain() {
        return new Promotion(id, organizationId, storeId, code, name,
                discountType, discountValue, startsAt, endsAt, status);
    }
}
