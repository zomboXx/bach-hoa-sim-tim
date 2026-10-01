package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.sale.domain.BatchStock;

/**
 * Public boundary port cho module inventory.
 * Cho phép các module khác (như sale) tra cứu lô hàng FEFO, trừ tồn kho
 * và ghi nhận biến động trong cùng transactional context.
 */
public interface InventoryPort {

    /**
     * Tra cứu các lô còn hạn theo FEFO (hạn dùng gần nhất xếp trước).
     * @param orgId ID tổ chức
     * @param storeId ID cửa hàng
     * @param productId ID sản phẩm
     * @return Danh sách các lô khả dụng kèm số lượng tồn hiện hành
     */
    List<BatchStock> findAvailableBatchesFEFO(UUID orgId, UUID storeId, UUID productId);

    /**
     * Khóa bi quan và trừ tồn kho của lô.
     * @throws vn.simtim.api.sale.domain.SaleConflictException nếu tồn kho không đủ
     */
    void deductBalance(UUID orgId, UUID storeId, UUID batchId, BigDecimal quantity);

    /**
     * Ghi nhận dòng biến động kho (SALE) khi xuất bán hàng.
     */
    void recordSaleMovement(UUID id, UUID orgId, UUID storeId, UUID batchId,
                            BigDecimal quantityDelta, UUID invoiceLineId,
                            UUID actorUserId, Instant occurredAt);
}
