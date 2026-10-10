package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "stock_movements")
class StockMovementJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(name = "batch_id", nullable = false) UUID batchId;
    @Column(name = "movement_type", nullable = false, length = 32) String movementType;
    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3) BigDecimal quantityDelta;
    @Column(name = "reference_id", nullable = false) UUID referenceId;
    @Column(name = "reference_type", nullable = false, length = 32) String referenceType;
    @Column(name = "occurred_at", nullable = false) Instant occurredAt;
    @Column(name = "recorded_by", nullable = false) UUID recordedBy;

    StockMovementJpa() {}
}
