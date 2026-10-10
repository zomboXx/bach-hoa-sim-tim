package vn.simtim.api.inventory.application;

import java.util.Optional;
import java.util.UUID;
import vn.simtim.api.inventory.domain.InventoryBalance;

/**
 * Cổng đọc balance theo (org, store, batch) — dùng bởi StocktakeService.
 * Tách khỏi GoodsReceiptRepository để giữ dependency rõ ràng.
 */
public interface InventoryBalanceReader {

    Optional<InventoryBalance> findByBatch(UUID organizationId, UUID storeId, UUID batchId);
}
