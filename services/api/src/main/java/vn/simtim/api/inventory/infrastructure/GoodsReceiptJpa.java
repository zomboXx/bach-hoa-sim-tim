package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "goods_receipts")
class GoodsReceiptJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "supplier_id", nullable = false) UUID supplierId;
    @Column(nullable = false, length = 16) String status;
    @Column(name = "received_at", nullable = false) Instant receivedAt;
    @Column(name = "confirmed_by", nullable = false) UUID confirmedBy;
    @Column(name = "client_operation_id", nullable = false) UUID clientOperationId;
    @Column(name = "idempotency_key", nullable = false) UUID idempotencyKey;
    @Column(name = "payload_hash", nullable = false) byte[] payloadHash;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    GoodsReceiptJpa() {}
}
