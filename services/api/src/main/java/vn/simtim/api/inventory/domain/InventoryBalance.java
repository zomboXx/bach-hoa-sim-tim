package vn.simtim.api.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Số dư tồn kho theo (organization, store, product, batch). */
public record InventoryBalance(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID productId,
        UUID batchId,
        BigDecimal onHandQuantity,
        long version) {
}
