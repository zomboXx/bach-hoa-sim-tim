package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GoodsReceiptJpaRepository extends JpaRepository<GoodsReceiptJpa, UUID> {

    @Query("SELECT r FROM GoodsReceiptJpa r WHERE r.organizationId=:orgId AND r.storeId=:storeId AND r.id=:id")
    Optional<GoodsReceiptJpa> findByScope(UUID orgId, UUID storeId, UUID id);

    @Query("SELECT r FROM GoodsReceiptJpa r WHERE r.organizationId=:orgId AND r.storeId=:storeId ORDER BY r.receivedAt DESC LIMIT :limit OFFSET :offset")
    List<GoodsReceiptJpa> findByStore(UUID orgId, UUID storeId, int limit, int offset);

    Optional<GoodsReceiptJpa> findByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT r FROM GoodsReceiptJpa r WHERE r.organizationId=:orgId AND r.clientOperationId=:clientOpId")
    Optional<GoodsReceiptJpa> findByClientOperationId(UUID orgId, UUID clientOpId);
}
