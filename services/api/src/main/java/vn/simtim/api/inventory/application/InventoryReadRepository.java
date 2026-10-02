package vn.simtim.api.inventory.application;

import java.time.LocalDate;
import java.util.UUID;

/** Outbound query port cho các màn hình tồn hiện tại. */
public interface InventoryReadRepository {

    InventoryPage<InventoryProductView> findProducts(
            UUID organizationId, UUID storeId, UUID productId, String productStatus,
            LocalDate businessDate, int page, int size);

    InventoryPage<InventoryBatchView> findBatches(
            UUID organizationId, UUID storeId, UUID productId, String batchStatus,
            ExpiryStatus expiryStatus, LocalDate businessDate, int page, int size);

    InventoryPage<InventoryMovementView> findMovements(
            UUID organizationId, UUID storeId, UUID productId, UUID batchId,
            String movementType, int page, int size);
}
