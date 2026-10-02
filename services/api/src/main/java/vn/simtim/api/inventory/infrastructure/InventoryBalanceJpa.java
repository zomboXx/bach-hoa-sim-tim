package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "inventory_balances")
class InventoryBalanceJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(name = "batch_id", nullable = false) UUID batchId;
    @Column(name = "on_hand_quantity", nullable = false, precision = 14, scale = 3) BigDecimal onHandQuantity;

    @Version
    @Column(nullable = false)
    long version;

    InventoryBalanceJpa() {}
}
