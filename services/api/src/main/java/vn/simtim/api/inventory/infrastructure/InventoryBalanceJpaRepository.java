package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceJpaRepository extends JpaRepository<InventoryBalanceJpa, UUID> {

    /**
     * Đặt khóa PESSIMISTIC_WRITE trên balance của một lô.
     * Phải gọi trong transaction đang mở.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBalanceJpa b WHERE b.organizationId = :orgId AND b.storeId = :storeId AND b.batchId = :batchId")
    Optional<InventoryBalanceJpa> findAndLockByBatchId(@Param("orgId") UUID orgId,
                                                        @Param("storeId") UUID storeId,
                                                        @Param("batchId") UUID batchId);

    /**
     * Danh sách balance có thể dùng FEFO, join với product_batches để lấy expiry_date.
     * Chỉ trả về lô AVAILABLE còn tồn dương.
     */
    @Query(value = """
            SELECT ib.batch_id, pb.product_id, ib.on_hand_quantity, pb.expiry_date
            FROM inventory.inventory_balances ib
            JOIN inventory.product_batches pb ON pb.id = ib.batch_id
            WHERE ib.organization_id = :orgId
              AND ib.store_id = :storeId
              AND pb.product_id = :productId
              AND pb.status = 'AVAILABLE'
              AND ib.on_hand_quantity > 0
              AND (pb.expiry_date IS NULL OR pb.expiry_date >= CURRENT_DATE)
            ORDER BY pb.expiry_date ASC NULLS LAST, pb.received_date ASC, pb.id ASC
            """, nativeQuery = true)
    List<Object[]> findAvailableBatchesFEFO(@Param("orgId") UUID orgId,
                                           @Param("storeId") UUID storeId,
                                           @Param("productId") UUID productId);
}
