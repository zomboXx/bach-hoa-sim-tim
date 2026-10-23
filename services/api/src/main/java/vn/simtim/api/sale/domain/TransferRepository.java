package vn.simtim.api.sale.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.simtim.api.inventory.application.SaleIssue;

public interface TransferRepository {
    Optional<TransferOrder> find(UUID id, boolean lock);
    Optional<TransferOrder> pending(UUID cartId);
    Optional<TransferOrder> operation(PosCart.Scope scope, UUID operationId);
    List<TransferOrder> list(UUID org, UUID store, UUID owner, int limit, int offset);
    Summary summary(UUID org, UUID store, UUID owner);
    List<UUID> expired(Instant now);
    void save(TransferOrder order);
    List<SaleIssue> allocations(TransferOrder order, UUID actor);
    boolean receiptUsed(TransferOrder order, String reference);
    void complete(TransferOrder order, UUID actor, String reference, Instant now);
    void release(TransferOrder order, String status, UUID actor, String note, Instant now);
    record Summary(long pending, long completed, long cancelled, long expired, long receivedAmount) {}
}
