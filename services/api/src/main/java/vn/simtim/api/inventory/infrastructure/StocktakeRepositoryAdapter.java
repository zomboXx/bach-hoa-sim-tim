package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.InventoryBalanceReader;
import vn.simtim.api.inventory.domain.*;

/**
 * JPA adapter cho StocktakeRepository + InventoryBalanceReader.
 * Giữ mapping domain ↔ JPA tập trung tại đây.
 */
@Repository
class StocktakeRepositoryAdapter implements StocktakeRepository, InventoryBalanceReader {

    private final StocktakeJpaRepository sessionJpa;
    private final StocktakeLineJpaRepository lineJpa;
    private final InventoryBalanceJpaRepository balanceJpa;

    StocktakeRepositoryAdapter(StocktakeJpaRepository sessionJpa,
                                StocktakeLineJpaRepository lineJpa,
                                InventoryBalanceJpaRepository balanceJpa) {
        this.sessionJpa = sessionJpa;
        this.lineJpa    = lineJpa;
        this.balanceJpa = balanceJpa;
    }

    // ── StocktakeRepository ───────────────────────────────────────────────────

    @Override
    @Transactional
    public Stocktake saveSession(Stocktake s) {
        var jpa = new StocktakeJpa();
        jpa.id             = s.id();
        jpa.organizationId = s.organizationId();
        jpa.storeId        = s.storeId();
        jpa.actorId        = s.actorId();
        jpa.status         = s.status();
        jpa.openedAt       = s.openedAt();
        jpa.submittedAt    = s.submittedAt();
        sessionJpa.save(jpa);
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stocktake> findOpenSession(UUID organizationId, UUID storeId, UUID actorId) {
        return sessionJpa.findOpen(organizationId, storeId, actorId)
                .map(j -> toSession(j, List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stocktake> findById(UUID organizationId, UUID storeId, UUID id) {
        return sessionJpa.findByScope(organizationId, storeId, id)
                .map(j -> {
                    var lines = lineJpa.findByStocktakeId(j.id)
                            .stream().map(this::toLine).toList();
                    return toSession(j, lines);
                });
    }

    @Override
    @Transactional
    public StocktakeLine saveLine(StocktakeLine l) {
        var existing = lineJpa.findByStocktakeIdAndBatchId(l.stocktakeId(), l.batchId());
        var jpa = existing.orElseGet(StocktakeLineJpa::new);
        if (existing.isEmpty()) {
            jpa.id                 = l.id();
            jpa.stocktakeId        = l.stocktakeId();
            jpa.organizationId     = l.organizationId();
            jpa.storeId            = l.storeId();
            jpa.productId          = l.productId();
            jpa.batchId            = l.batchId();
        }
        jpa.clientOperationId  = l.clientOperationId();
        jpa.expectedQuantity   = l.expectedQuantity();
        jpa.actualQuantity     = l.actualQuantity();
        jpa.baseVersion        = l.baseVersion();
        jpa.note               = l.note();
        jpa.status             = l.status();
        jpa.conflictReason     = l.conflictReason();
        jpa.countedAt          = l.countedAt();
        lineJpa.save(jpa);
        return toLine(jpa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeLine> findLines(UUID stocktakeId) {
        return lineJpa.findByStocktakeId(stocktakeId).stream().map(this::toLine).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StocktakeLine> findLineByClientOperationId(UUID organizationId,
                                                                UUID storeId,
                                                                UUID clientOperationId) {
        return lineJpa.findByClientOperationId(organizationId, storeId, clientOperationId)
                .map(this::toLine);
    }

    @Override
    @Transactional
    public void updateLineStatus(UUID lineId, String status, String conflictReason) {
        lineJpa.findById(lineId).ifPresent(jpa -> {
            jpa.status = status;
            jpa.conflictReason = conflictReason;
            lineJpa.save(jpa);
        });
    }

    // ── InventoryBalanceReader ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryBalance> findByBatch(UUID organizationId,
                                                   UUID storeId,
                                                   UUID batchId) {
        return balanceJpa.findByScope(organizationId, storeId, batchId)
                .map(b -> new InventoryBalance(b.id, b.organizationId, b.storeId,
                        b.productId, b.batchId, b.onHandQuantity, b.version));
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private Stocktake toSession(StocktakeJpa j, List<StocktakeLine> lines) {
        return new Stocktake(j.id, j.organizationId, j.storeId, j.actorId,
                j.status, j.openedAt, j.submittedAt, lines);
    }

    private StocktakeLine toLine(StocktakeLineJpa l) {
        return new StocktakeLine(l.id, l.stocktakeId, l.organizationId, l.storeId,
                l.productId, l.batchId, l.clientOperationId,
                l.expectedQuantity, l.actualQuantity, l.baseVersion,
                l.note, l.status, l.conflictReason, l.countedAt);
    }
}
