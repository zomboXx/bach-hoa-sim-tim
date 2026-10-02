package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "product_batches")
class ProductBatchJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(name = "receipt_line_id", nullable = false) UUID receiptLineId;
    @Column(name = "batch_number", nullable = false, length = 100) String batchNumber;
    @Column(name = "supplier_lot_number") String supplierLotNumber;
    @Column(name = "expiry_date") LocalDate expiryDate;
    @Column(name = "received_date", nullable = false) LocalDate receivedDate;
    @Column(nullable = false, length = 16) String status;

    ProductBatchJpa() {}
}
