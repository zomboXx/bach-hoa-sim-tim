package vn.simtim.api.inventory.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.simtim.api.inventory.application.BatchStock;
import vn.simtim.api.inventory.application.InventoryConflictException;
import vn.simtim.api.inventory.application.InventoryPort;

/**
 * Adapter thực thi InventoryPort.
 * Cung cấp khả năng giao tiếp giữa các module với inventory schema
 * trong cùng database transaction context.
 */
@Component
public class InventoryPortAdapter implements InventoryPort {

    private final InventoryBalanceJpaRepository balanceRepo;
    private final JdbcTemplate jdbc;

    public InventoryPortAdapter(InventoryBalanceJpaRepository balanceRepo, JdbcTemplate jdbc) {
        this.balanceRepo = balanceRepo;
        this.jdbc = jdbc;
    }

    @Override
    public List<BatchStock> findAvailableBatchesFEFO(UUID orgId, UUID storeId, UUID productId) {
        return balanceRepo.findAvailableBatchesFEFO(orgId, storeId, productId).stream()
                .map(row -> new BatchStock(
                        UUID.fromString(row[0].toString()),
                        UUID.fromString(row[1].toString()),
                        new BigDecimal(row[2].toString()),
                        row[3] != null ? LocalDate.parse(row[3].toString()) : null))
                .toList();
    }

    @Override
    public void deductBalance(UUID orgId, UUID storeId, UUID batchId, BigDecimal quantity) {
        var balance = balanceRepo.findAndLockByBatchId(orgId, storeId, batchId)
                .orElseThrow(() -> new InventoryConflictException(
                        "Không tìm thấy số dư tồn kho cho lô: " + batchId));
        BigDecimal newQty = balance.getQuantityOnHand().subtract(quantity);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new InventoryConflictException(
                    "Tồn kho không đủ cho lô " + batchId + " (còn " + balance.getQuantityOnHand() + ", cần " + quantity + ")");
        }
        balance.setQuantityOnHand(newQty);
        balanceRepo.save(balance);
    }

    @Override
    public void recordSaleMovement(UUID id, UUID orgId, UUID storeId, UUID batchId,
                                   BigDecimal quantityDelta, UUID invoiceLineId,
                                   UUID actorUserId, Instant occurredAt) {
        UUID productId = jdbc.queryForObject(
                "SELECT product_id FROM inventory.product_batches WHERE id = ?",
                UUID.class, batchId);
        jdbc.update("""
                INSERT INTO inventory.stock_movements
                    (id, organization_id, store_id, product_id, batch_id, movement_type,
                     quantity_delta, reference_id, reference_type, occurred_at, recorded_by)
                VALUES (?, ?, ?, ?, ?, 'SALE', ?, ?, 'INVOICE', ?, ?)
                """,
                id, orgId, storeId, productId, batchId,
                quantityDelta, invoiceLineId,
                java.sql.Timestamp.from(occurredAt), actorUserId);
    }
}
