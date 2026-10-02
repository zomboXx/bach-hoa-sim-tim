package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.domain.*;

/**
 * Use-case INV-01: xác nhận phiếu nhận hàng.
 * Tạo phiếu, dòng phiếu, lô, số dư và biến động trong một transaction nguyên tử.
 */
@Service
@Transactional
public class GoodsReceiptService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final GoodsReceiptRepository receiptRepo;
    private final ProductExistenceChecker productChecker;
    private final InventoryProductLocker productLocker;

    public GoodsReceiptService(GoodsReceiptRepository receiptRepo,
                               ProductExistenceChecker productChecker,
                               InventoryProductLocker productLocker) {
        this.receiptRepo = receiptRepo;
        this.productChecker = productChecker;
        this.productLocker = productLocker;
    }

    /**
     * Xác nhận phiếu nhận hàng nguyên tử.
     *
     * @param cmd         lệnh tạo phiếu từ HTTP layer
     * @param principal   người dùng thực hiện (từ session)
     * @param idempotencyKey UUID từ header Idempotency-Key
     * @return phiếu đã CONFIRMED
     */
    public GoodsReceipt confirm(ConfirmReceiptCommand cmd,
                                UUID actorId, UUID organizationId, UUID storeId,
                                UUID idempotencyKey) {

        // 1. Idempotency: cùng key → trả chứng từ cũ
        Optional<GoodsReceipt> existing = receiptRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            GoodsReceipt old = existing.get();
            // Cùng payload → trả lại, khác payload → 409
            byte[] newHash = sha256(cmd.payloadJson());
            byte[] oldHash = receiptRepo.findPayloadHash(idempotencyKey);
            if (!MessageDigest.isEqual(newHash, oldHash)) {
                throw new InventoryReceiptException("IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã dùng với payload khác");
            }
            return old;
        }

        // 2. Validate từng dòng
        Instant now = Instant.now();
        LocalDate businessDate = now.atZone(VN).toLocalDate();
        validateLines(cmd.lines(), organizationId, storeId, businessDate);
        List<UUID> productIds = cmd.lines().stream()
                .map(ConfirmReceiptCommand.LineCmd::productId)
                .distinct()
                .sorted()
                .toList();
        if (!productLocker.lockActiveProducts(organizationId, productIds).equals(productIds)) {
            throw new InventoryReceiptException(
                    "NOT_FOUND", "Sản phẩm không tồn tại hoặc không hoạt động");
        }

        // 3. Tạo domain objects
        UUID receiptId = UUID.randomUUID();
        List<ReceiptLine> lines = new ArrayList<>();
        List<ProductBatch> batches = new ArrayList<>();
        List<InventoryBalance> balances = new ArrayList<>();

        for (ConfirmReceiptCommand.LineCmd lc : cmd.lines()) {
            UUID lineId = UUID.randomUUID();
            var line = new ReceiptLine(
                    lineId, receiptId, organizationId, lc.productId(),
                    lc.expectedQuantity(), lc.deliveredQuantity(),
                    lc.acceptedQuantity(), lc.rejectedQuantity(),
                    lc.unitCost(), lc.supplierLotNumber(), lc.expiryDate(),
                    lc.discrepancyReason());
            lines.add(line);

            // accepted > 0 → tạo lô và số dư
            if (line.createsStock()) {
                UUID batchId = UUID.randomUUID();
                String batchNumber = "BATCH-" + batchId.toString().substring(0, 8).toUpperCase();
                var batch = new ProductBatch(batchId, organizationId, storeId,
                        lc.productId(), lineId, batchNumber,
                        lc.supplierLotNumber(), lc.expiryDate(), businessDate, "AVAILABLE");
                batches.add(batch);

                var balance = new InventoryBalance(UUID.randomUUID(),
                        organizationId, storeId, lc.productId(), batchId,
                        lc.acceptedQuantity(), 0L);
                balances.add(balance);
            }
        }

        var receipt = new GoodsReceipt(receiptId, organizationId, storeId,
                cmd.supplierId(), "CONFIRMED", now, actorId,
                cmd.clientOperationId(), lines);

        // 4. Lưu nguyên tử (receipt + lines + batches + balances + movements) trong repo
        byte[] payloadHash = sha256(cmd.payloadJson());
        return receiptRepo.save(receipt, batches, balances, idempotencyKey, payloadHash);
    }

    private void validateLines(List<ConfirmReceiptCommand.LineCmd> lines,
                               UUID organizationId, UUID storeId, LocalDate businessDate) {
        if (lines == null || lines.isEmpty()) {
            throw new InventoryReceiptException("INVALID_RECEIPT", "Phiếu phải có ít nhất một dòng");
        }
        for (int i = 0; i < lines.size(); i++) {
            var lc = lines.get(i);
            String prefix = "lines[" + i + "]";

            // accepted + rejected = delivered
            if (lc.acceptedQuantity().add(lc.rejectedQuantity())
                    .compareTo(lc.deliveredQuantity()) != 0) {
                throw new InventoryReceiptException("INVALID_RECEIPT",
                        prefix + ": accepted + rejected phải bằng delivered");
            }
            // discrepancy reason required
            boolean hasDiscrepancy = lc.rejectedQuantity().compareTo(BigDecimal.ZERO) > 0
                    || lc.deliveredQuantity().compareTo(lc.expectedQuantity()) != 0;
            if (hasDiscrepancy && (lc.discrepancyReason() == null || lc.discrepancyReason().isBlank())) {
                throw new InventoryReceiptException("INVALID_RECEIPT",
                        prefix + ": cần discrepancyReason khi có sai lệch hoặc từ chối");
            }
            // expiryDate must not be before businessDate for expiry-tracked products
            if (lc.expiryDate() != null && lc.expiryDate().isBefore(businessDate)) {
                throw new InventoryReceiptException("INVALID_RECEIPT",
                        prefix + ".expiryDate: hạn sử dụng không được trước ngày nhận hàng");
            }
            // product must exist and be active in this org
            if (!productChecker.isActive(organizationId, lc.productId())) {
                throw new InventoryReceiptException("NOT_FOUND",
                        prefix + ".productId: sản phẩm không tồn tại hoặc không hoạt động");
            }
        }
    }

    @Transactional(readOnly = true)
    public GoodsReceipt getById(UUID organizationId, UUID storeId, UUID id) {
        return receiptRepo.findById(organizationId, storeId, id)
                .orElseThrow(() -> new InventoryReceiptException("NOT_FOUND",
                        "Phiếu nhận hàng không tồn tại: " + id));
    }

    @Transactional(readOnly = true)
    public List<GoodsReceipt> list(UUID organizationId, UUID storeId, int limit, int offset) {
        return receiptRepo.findByStore(organizationId, storeId, limit, offset);
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
