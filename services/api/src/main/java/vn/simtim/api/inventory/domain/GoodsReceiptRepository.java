package vn.simtim.api.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port domain cho phiếu nhận hàng — thực thi bởi infrastructure adapter. */
public interface GoodsReceiptRepository {
    GoodsReceipt save(GoodsReceipt receipt, List<ProductBatch> batches, List<InventoryBalance> balances,
                      UUID idempotencyKey, byte[] payloadHash);
    Optional<GoodsReceipt> findById(UUID organizationId, UUID storeId, UUID id);
    List<GoodsReceipt> findByStore(UUID organizationId, UUID storeId, int limit, int offset);
    Optional<GoodsReceipt> findByClientOperationId(UUID organizationId, UUID storeId, UUID clientOperationId);
    Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey);
    byte[] findPayloadHash(UUID idempotencyKey);
}
