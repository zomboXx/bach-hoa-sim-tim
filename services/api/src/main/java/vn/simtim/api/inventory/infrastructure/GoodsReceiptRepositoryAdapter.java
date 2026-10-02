package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.domain.*;

/**
 * JPA adapter cho GoodsReceiptRepository.
 * Lưu receipt + lines + batches + balances + movements trong cùng một transaction.
 */
@Repository
class GoodsReceiptRepositoryAdapter implements GoodsReceiptRepository {

    private final GoodsReceiptJpaRepository receiptJpa;
    private final GoodsReceiptLineJpaRepository lineJpa;
    private final ProductBatchJpaRepository batchJpa;
    private final InventoryBalanceJpaRepository balanceJpa;
    private final StockMovementJpaRepository movementJpa;
    private final EntityManager em;

    GoodsReceiptRepositoryAdapter(GoodsReceiptJpaRepository receiptJpa,
                                  GoodsReceiptLineJpaRepository lineJpa,
                                  ProductBatchJpaRepository batchJpa,
                                  InventoryBalanceJpaRepository balanceJpa,
                                  StockMovementJpaRepository movementJpa,
                                  EntityManager em) {
        this.receiptJpa = receiptJpa;
        this.lineJpa    = lineJpa;
        this.batchJpa   = batchJpa;
        this.balanceJpa = balanceJpa;
        this.movementJpa = movementJpa;
        this.em = em;
    }

    @Override
    @Transactional
    public GoodsReceipt save(GoodsReceipt receipt, List<ProductBatch> batches,
                             List<InventoryBalance> balances,
                             UUID idempotencyKey, byte[] payloadHash) {
        Instant now = Instant.now();

        // 1. Receipt header
        var rJpa = new GoodsReceiptJpa();
        rJpa.id                 = receipt.id();
        rJpa.organizationId     = receipt.organizationId();
        rJpa.storeId            = receipt.storeId();
        rJpa.supplierId         = receipt.supplierId();
        rJpa.status             = receipt.status();
        rJpa.receivedAt         = receipt.receivedAt();
        rJpa.confirmedBy        = receipt.confirmedBy();
        rJpa.clientOperationId  = receipt.clientOperationId();
        rJpa.idempotencyKey     = idempotencyKey;
        rJpa.payloadHash        = payloadHash;
        rJpa.createdAt          = now;
        receiptJpa.save(rJpa);

        // 2. Lines
        for (ReceiptLine line : receipt.lines()) {
            var lJpa = new GoodsReceiptLineJpa();
            lJpa.id                  = line.id();
            lJpa.receiptId           = line.receiptId();
            lJpa.organizationId      = line.organizationId();
            lJpa.productId           = line.productId();
            lJpa.expectedQuantity    = line.expectedQuantity();
            lJpa.deliveredQuantity   = line.deliveredQuantity();
            lJpa.acceptedQuantity    = line.acceptedQuantity();
            lJpa.rejectedQuantity    = line.rejectedQuantity();
            lJpa.unitCost            = line.unitCost();
            lJpa.supplierLotNumber   = line.supplierLotNumber();
            lJpa.expiryDate          = line.expiryDate();
            lJpa.discrepancyReason   = line.discrepancyReason();
            lineJpa.save(lJpa);
        }

        // 3. Batches
        for (ProductBatch batch : batches) {
            var bJpa = new ProductBatchJpa();
            bJpa.id                = batch.id();
            bJpa.organizationId    = batch.organizationId();
            bJpa.storeId           = batch.storeId();
            bJpa.productId         = batch.productId();
            bJpa.receiptLineId     = batch.receiptLineId();
            bJpa.batchNumber       = batch.batchNumber();
            bJpa.supplierLotNumber = batch.supplierLotNumber();
            bJpa.expiryDate        = batch.expiryDate();
            bJpa.receivedDate      = batch.receivedDate();
            bJpa.status            = batch.status();
            batchJpa.save(bJpa);
        }

        // 4. Balances + stock movements (RECEIPT)
        for (int i = 0; i < batches.size(); i++) {
            ProductBatch batch   = batches.get(i);
            InventoryBalance bal = balances.get(i);

            var balJpa = new InventoryBalanceJpa();
            balJpa.id             = bal.id();
            balJpa.organizationId = bal.organizationId();
            balJpa.storeId        = bal.storeId();
            balJpa.productId      = bal.productId();
            balJpa.batchId        = bal.batchId();
            balJpa.onHandQuantity = bal.onHandQuantity();
            balanceJpa.save(balJpa);

            var mvJpa = new StockMovementJpa();
            mvJpa.id            = UUID.randomUUID();
            mvJpa.organizationId = receipt.organizationId();
            mvJpa.storeId       = receipt.storeId();
            mvJpa.productId     = batch.productId();
            mvJpa.batchId       = batch.id();
            mvJpa.movementType  = "RECEIPT";
            mvJpa.quantityDelta = bal.onHandQuantity();
            mvJpa.referenceId   = receipt.id();
            mvJpa.referenceType = "GOODS_RECEIPT";
            mvJpa.occurredAt    = now;
            mvJpa.recordedBy    = receipt.confirmedBy();
            movementJpa.save(mvJpa);
        }

        em.flush();
        return receipt;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoodsReceipt> findById(UUID organizationId, UUID storeId, UUID id) {
        return receiptJpa.findByScope(organizationId, storeId, id)
                .map(r -> toReceipt(r, lineJpa.findByReceiptId(r.id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceipt> findByStore(UUID organizationId, UUID storeId, int limit, int offset) {
        return receiptJpa.findByStore(organizationId, storeId, limit, offset)
                .stream()
                .map(r -> toReceipt(r, lineJpa.findByReceiptId(r.id)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoodsReceipt> findByClientOperationId(UUID organizationId, UUID clientOperationId) {
        return receiptJpa.findByClientOperationId(organizationId, clientOperationId)
                .map(r -> toReceipt(r, lineJpa.findByReceiptId(r.id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoodsReceipt> findByIdempotencyKey(UUID idempotencyKey) {
        return receiptJpa.findByIdempotencyKey(idempotencyKey)
                .map(r -> toReceipt(r, lineJpa.findByReceiptId(r.id)));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] findPayloadHash(UUID idempotencyKey) {
        return receiptJpa.findByIdempotencyKey(idempotencyKey)
                .map(r -> r.payloadHash)
                .orElse(new byte[0]);
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private GoodsReceipt toReceipt(GoodsReceiptJpa r, List<GoodsReceiptLineJpa> ls) {
        var lines = ls.stream().map(this::toLine).toList();
        return new GoodsReceipt(r.id, r.organizationId, r.storeId, r.supplierId,
                r.status, r.receivedAt, r.confirmedBy, r.clientOperationId, lines);
    }

    private ReceiptLine toLine(GoodsReceiptLineJpa l) {
        return new ReceiptLine(l.id, l.receiptId, l.organizationId, l.productId,
                l.expectedQuantity, l.deliveredQuantity, l.acceptedQuantity,
                l.rejectedQuantity, l.unitCost, l.supplierLotNumber,
                l.expiryDate, l.discrepancyReason);
    }
}
