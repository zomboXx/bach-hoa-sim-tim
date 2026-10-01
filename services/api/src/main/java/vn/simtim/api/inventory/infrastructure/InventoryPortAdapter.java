package vn.simtim.api.inventory.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.simtim.api.inventory.application.InventoryPort;
import vn.simtim.api.sale.domain.BatchStock;
import vn.simtim.api.sale.domain.SaleConflictException;

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
                .orElseThrow(() -> new SaleConflictException(
                        "Không tìm thấy số dư tồn kho cho lô: " + batchId));
        BigDecimal newQty = balance.getQuantityOnHand().subtract(quantity);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaleConflictException(
                    "Tồn kho không đủ cho lô " + batchId + " (còn " + balance.getQuantityOnHand() + ", cần " + quantity + ")");
        }
        balance.setQuantityOnHand(newQty);
        balanceRepo.save(balance);
    }

    @Override
    public void recordSaleMovement(UUID id, UUID orgId, UUID storeId, UUID batchId,
                                   BigDecimal quantityDelta, UUID invoiceLineId,
                                   UUID actorUserId, Instant occurredAt) {
        jdbc.update("""
                INSERT INTO inventory.stock_movements
                    (id, organization_id, store_id, product_batch_id, movement_type,
                     quantity_delta, invoice_line_id, occurred_at, actor_user_id)
                VALUES (?, ?, ?, ?, 'SALE', ?, ?, ?, ?)
                """,
                id, orgId, storeId, batchId,
                quantityDelta, invoiceLineId,
                java.sql.Timestamp.from(occurredAt), actorUserId);
    }
}
