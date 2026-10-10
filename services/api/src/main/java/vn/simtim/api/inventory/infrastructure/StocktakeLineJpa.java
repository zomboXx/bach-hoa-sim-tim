package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "stocktake_lines")
class StocktakeLineJpa {

    @Id UUID id;
    @Column(name = "stocktake_id",          nullable = false) UUID stocktakeId;
    @Column(name = "organization_id",        nullable = false) UUID organizationId;
    @Column(name = "store_id",               nullable = false) UUID storeId;
    @Column(name = "product_id",             nullable = false) UUID productId;
    @Column(name = "batch_id",               nullable = false) UUID batchId;
    @Column(name = "client_operation_id",    nullable = false) UUID clientOperationId;
    @Column(name = "expected_quantity",      nullable = false, precision = 14, scale = 3)
    BigDecimal expectedQuantity;
    @Column(name = "actual_quantity",        nullable = false, precision = 14, scale = 3)
    BigDecimal actualQuantity;
    @Column(name = "base_version",           nullable = false) long baseVersion;
    @Column(name = "note",                   nullable = false) String note;
    @Column(name = "status",                 nullable = false) String status;
    @Column(name = "conflict_reason")                          String conflictReason;
    @Column(name = "counted_at",             nullable = false) Instant countedAt;

    StocktakeLineJpa() {}
}
