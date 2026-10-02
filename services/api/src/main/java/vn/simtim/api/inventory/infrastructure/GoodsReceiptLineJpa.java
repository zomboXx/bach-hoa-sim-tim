package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "goods_receipt_lines")
class GoodsReceiptLineJpa {

    @Id UUID id;
    @Column(name = "receipt_id", nullable = false)     UUID receiptId;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "product_id", nullable = false)      UUID productId;
    @Column(name = "expected_quantity", nullable = false, precision = 14, scale = 3)  BigDecimal expectedQuantity;
    @Column(name = "delivered_quantity", nullable = false, precision = 14, scale = 3) BigDecimal deliveredQuantity;
    @Column(name = "accepted_quantity", nullable = false, precision = 14, scale = 3)  BigDecimal acceptedQuantity;
    @Column(name = "rejected_quantity", nullable = false, precision = 14, scale = 3)  BigDecimal rejectedQuantity;
    @Column(name = "unit_cost", nullable = false)  long unitCost;
    @Column(name = "supplier_lot_number") String supplierLotNumber;
    @Column(name = "expiry_date")          LocalDate expiryDate;
    @Column(name = "discrepancy_reason", length = 500) String discrepancyReason;

    GoodsReceiptLineJpa() {}
}
