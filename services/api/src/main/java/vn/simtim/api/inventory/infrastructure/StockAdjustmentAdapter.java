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
    public int getProductPrecisionScale(UUID orgId, UUID productId) {
        var list = jdbc.queryForList("""
                SELECT u.precision_scale
                FROM catalog.products p
                JOIN catalog.units u ON p.organization_id = u.organization_id AND p.base_unit_id = u.id
                WHERE p.organization_id = ? AND p.id = ?
                """, Integer.class, orgId, productId);
        if (list.isEmpty() || list.getFirst() == null) {
            return 3;
        }
        return list.getFirst();
    }

    @Override
    public void updateBalance(UUID orgId, UUID storeId, UUID batchId, BigDecimal newOnHandQuantity) {
        var jpa = balanceRepo.findAndLockByBatchId(orgId, storeId, batchId)
                .orElseThrow(() -> new IllegalStateException("Balance không tồn tại khi cập nhật: " + batchId));
        var held = jdbc.queryForObject("""
                select reserved_quantity from inventory.inventory_balances
                where organization_id=? and store_id=? and batch_id=?
                """, BigDecimal.class, orgId, storeId, batchId);
        if (held != null && newOnHandQuantity.compareTo(held) < 0) {
            throw new vn.simtim.api.inventory.domain.StocktakeException("STOCK_RESERVED",
                    "Tồn kiểm kê nhỏ hơn số đang giữ cho chuyển khoản; hãy đối chiếu giao dịch trước.");
        }
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
