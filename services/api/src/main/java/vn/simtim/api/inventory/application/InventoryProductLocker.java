package vn.simtim.api.inventory.application;

import java.util.List;
import java.util.UUID;

/** Khóa product theo UUID tăng dần để receipt và checkout dùng chung thứ tự. */
public interface InventoryProductLocker {
    List<UUID> lockActiveProducts(UUID organizationId, List<UUID> sortedProductIds);
}
