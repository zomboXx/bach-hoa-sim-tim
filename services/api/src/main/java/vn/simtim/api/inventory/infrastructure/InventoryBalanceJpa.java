package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity cho inventory.inventory_balances.
 * Dùng @Version để hỗ trợ optimistic locking và @Lock PESSIMISTIC_WRITE khi thao tác xuất nhập kho.
 */
@Entity
@Table(schema = "inventory", name = "inventory_balances")
class InventoryBalanceJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "product_batch_id", nullable = false, unique = true) UUID productBatchId;
    @Column(name = "quantity_on_hand", nullable = false, precision = 14, scale = 3)
    BigDecimal quantityOnHand;
    @Version @Column(nullable = false) long version;

    InventoryBalanceJpa() {}

    UUID getProductBatchId() { return productBatchId; }
    BigDecimal getQuantityOnHand() { return quantityOnHand; }
    void setQuantityOnHand(BigDecimal qty) { this.quantityOnHand = qty; }
}
