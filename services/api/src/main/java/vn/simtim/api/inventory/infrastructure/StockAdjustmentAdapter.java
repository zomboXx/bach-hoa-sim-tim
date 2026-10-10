package vn.simtim.api.inventory.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.StockAdjustmentPort;
import vn.simtim.api.inventory.domain.InventoryBalance;

/**
 * Triển khai StockAdjustmentPort bằng JPA Repositories và JdbcTemplate.
 */
@Component
@Transactional
class StockAdjustmentAdapter implements StockAdjustmentPort {

    private final InventoryBalanceJpaRepository balanceRepo;
    private final StockMovementJpaRepository movementRepo;
    private final JdbcTemplate jdbc;

    StockAdjustmentAdapter(InventoryBalanceJpaRepository balanceRepo,
                           StockMovementJpaRepository movementRepo,
                           JdbcTemplate jdbc) {
        this.balanceRepo = balanceRepo;
        this.movementRepo = movementRepo;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<InventoryBalance> findAndLockBalance(UUID orgId, UUID storeId, UUID batchId) {
        return balanceRepo.findAndLockByBatchId(orgId, storeId, batchId)
                .map(b -> new InventoryBalance(b.id, b.organizationId, b.storeId,
                        b.productId, b.batchId, b.onHandQuantity, b.version));
    }

    @Override
    public void updateBalance(UUID orgId, UUID storeId, UUID batchId, BigDecimal newOnHandQuantity) {
        var jpa = balanceRepo.findAndLockByBatchId(orgId, storeId, batchId)
                .orElseThrow(() -> new IllegalStateException("Balance không tồn tại khi cập nhật: " + batchId));
        jpa.setQuantityOnHand(newOnHandQuantity);
        balanceRepo.save(jpa);
    }

    @Override
    public void recordAdjustmentMovement(UUID orgId, UUID storeId, UUID productId, UUID batchId,
                                         BigDecimal quantityDelta, UUID stocktakeId, UUID actorId, Instant occurredAt) {
        var mv = new StockMovementJpa();
        mv.id = UUID.randomUUID();
        mv.organizationId = orgId;
        mv.storeId = storeId;
        mv.productId = productId;
        mv.batchId = batchId;
        mv.movementType = "STOCKTAKE_ADJUSTMENT";
        mv.quantityDelta = quantityDelta;
        mv.referenceId = stocktakeId;
        mv.referenceType = "STOCKTAKE";
        mv.occurredAt = occurredAt;
        mv.recordedBy = actorId;
        movementRepo.save(mv);
    }

    @Override
    public void recordAuditLog(UUID orgId, UUID actorId, String action, String entityType,
                               UUID entityId, String detail) {
        jdbc.update("""
                INSERT INTO audit.audit_logs (organization_id, actor_id, action, entity_type, entity_id, detail)
                VALUES (?, ?, ?, ?, ?, ?::jsonb)
                """, orgId, actorId, action, entityType, entityId, detail != null ? detail : "{}");
    }
}
