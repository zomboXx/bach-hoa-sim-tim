package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.simtim.api.inventory.domain.InventoryBalance;

/**
 * Port cho việc điều chỉnh tồn kho, ghi nhận stock movement và audit log
 * khi quản lý duyệt phiếu kiểm kê (INV-03).
 */
public interface StockAdjustmentPort {

    /** Khóa và đọc balance hiện hành của một lô trong cửa hàng. */
    Optional<InventoryBalance> findAndLockBalance(UUID organizationId, UUID storeId, UUID batchId);

    /** Cập nhật số lượng tồn kho mới cho lô (tự động tăng version). */
    void updateBalance(UUID organizationId, UUID storeId, UUID batchId, BigDecimal newOnHandQuantity);

    /** Ghi nhận biến động kiểm kê STOCKTAKE_ADJUSTMENT vào ledger. */
    void recordAdjustmentMovement(UUID organizationId, UUID storeId, UUID productId, UUID batchId,
                                  BigDecimal quantityDelta, UUID stocktakeId, UUID actorId, Instant occurredAt);

    /** Ghi audit log sự kiện duyệt kiểm kê. */
    void recordAuditLog(UUID organizationId, UUID actorId, String action, String entityType,
                        UUID entityId, String detail);
}
