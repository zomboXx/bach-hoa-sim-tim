package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.domain.*;

/**
 * Use-case SYN-02: API đồng bộ kiểm kê và xử lý xung đột.
 *
 * <p>Hai hành vi chính:
 * <ol>
 *   <li>{@link #openOrGetSession} — lấy phiên OPEN hiện tại hoặc tạo mới.</li>
 *   <li>{@link #submitCount} — ghi số đếm thực tế cho một lô, kiểm tra version
 *       và phát hiện xung đột; retry cùng clientOperationId+scope+payload
 *       trả lại kết quả cũ mà không ghi thêm.</li>
 * </ol>
 *
 * <p>Quy tắc idempotency (từ SYN-01 contract):
 * <ul>
 *   <li>Cùng clientOperationId + org/store/actor + payload → trả dòng cũ.</li>
 *   <li>Cùng key nhưng khác actor/scope/payload → 409 IDEMPOTENCY_KEY_REUSED.</li>
 *   <li>Khi balance.version != baseVersion tại lúc nhận → trả 409 CONFLICT.</li>
 * </ul>
 *
 * <p>Không tự động cập nhật tồn kho — việc đó thuộc INV-03 (duyệt).
 */
@Service
@Transactional
public class StocktakeService {

    private final StocktakeRepository stocktakeRepo;
    private final InventoryBalanceReader balanceReader;
    private final StockAdjustmentPort adjustmentPort;

    public StocktakeService(StocktakeRepository stocktakeRepo,
                            InventoryBalanceReader balanceReader,
                            StockAdjustmentPort adjustmentPort) {
        this.stocktakeRepo  = stocktakeRepo;
        this.balanceReader   = balanceReader;
        this.adjustmentPort = adjustmentPort;
    }

    // ── Open / get session ────────────────────────────────────────────────────

    /**
     * Trả phiên OPEN hiện tại của actor tại store; tạo mới nếu chưa có.
     * Quyền đã kiểm tra ở controller layer.
     */
    @Transactional
    public Stocktake openOrGetSession(UUID organizationId, UUID storeId, UUID actorId) {
        return stocktakeRepo.findOpenSession(organizationId, storeId, actorId)
                .orElseGet(() -> {
                    var session = Stocktake.open(organizationId, storeId, actorId);
                    return stocktakeRepo.saveSession(session);
                });
    }

    /**
     * Lấy phiên theo id — chỉ trong phạm vi org/store của session HTTP.
     */
    @Transactional(readOnly = true)
    public Stocktake getSession(UUID organizationId, UUID storeId, UUID sessionId) {
        return stocktakeRepo.findById(organizationId, storeId, sessionId)
                .orElseThrow(() -> new StocktakeException("NOT_FOUND",
                        "Phiên kiểm kê không tồn tại: " + sessionId));
    }

    /**
     * Lấy danh sách dòng của phiên (trong phạm vi org/store).
     */
    @Transactional(readOnly = true)
    public List<StocktakeLine> getLines(UUID organizationId, UUID storeId, UUID sessionId) {
        // Validate scope trước khi trả lines.
        getSession(organizationId, storeId, sessionId);
        return stocktakeRepo.findLines(sessionId);
    }

    // ── Submit count ──────────────────────────────────────────────────────────

    /**
     * Ghi số đếm thực tế cho một lô trong phiên.
     *
     * <p>Idempotency: nếu {@code clientOperationId} đã có trong phạm vi
     * org/store và cùng actor → trả lại dòng hiện tại.
     * Nếu cùng key nhưng khác actor hoặc scope → 409.
     *
     * <p>Conflict: nếu balance.version != cmd.baseVersion → trả CONFLICT
     * (không ghi adjustment, không ghi đè tồn).
     *
     * @param sessionId  id phiên đang OPEN của actor
     * @param actorId    actor đã xác thực từ session
     * @param cmd        lệnh chứa batchId, actualQuantity, baseVersion, note
     * @return dòng kiểm kê đã lưu (PENDING hoặc CONFLICT)
     */
    @Transactional
    public StocktakeLine submitCount(UUID organizationId, UUID storeId,
                                     UUID actorId, UUID sessionId,
                                     SubmitCountCommand cmd) {

        // 1. Validate phiên tồn tại và OPEN trong đúng scope
        Stocktake session = stocktakeRepo.findById(organizationId, storeId, sessionId)
                .orElseThrow(() -> new StocktakeException("NOT_FOUND",
                        "Phiên kiểm kê không tồn tại: " + sessionId));

        if (!session.isOpen()) {
            throw new StocktakeException("SESSION_CLOSED",
                    "Phiên kiểm kê đã đóng, không thể gửi số đếm mới.");
        }

        if (!session.actorId().equals(actorId)) {
            throw new StocktakeException("SCOPE_MISMATCH",
                    "Phiên thuộc tài khoản khác; không thể gửi số đếm.");
        }

        // 2. Idempotency: tìm dòng theo clientOperationId trong phạm vi org/store
        Optional<StocktakeLine> existing = stocktakeRepo.findLineByClientOperationId(
                organizationId, storeId, cmd.clientOperationId());

        if (existing.isPresent()) {
            StocktakeLine old = existing.get();
            // Cùng phiên/actor → trả lại (idempotent retry)
            if (old.stocktakeId().equals(sessionId)) {
                return old;
            }
            // Khác phiên → key bị tái dùng sai scope
            throw new StocktakeException("IDEMPOTENCY_KEY_REUSED",
                    "clientOperationId đã dùng trong phiên khác.");
        }

        // 3. Lấy balance theo batchId trong phạm vi org/store
        InventoryBalance balance = balanceReader
                .findByBatch(organizationId, storeId, cmd.batchId())
                .orElseThrow(() -> new StocktakeException("NOT_FOUND",
                        "Lô hàng không tìm thấy trong cửa hàng hiện tại: " + cmd.batchId()));

        // 4. Phát hiện xung đột — version lệch
        String status;
        String conflictReason = null;

        if (balance.version() != cmd.baseVersion()) {
            status = "CONFLICT";
            conflictReason = String.format(
                    "Tồn kho lô %s đã thay đổi (baseVersion=%d, currentVersion=%d). "
                            + "Hãy lấy snapshot mới và kiểm lại.",
                    cmd.batchId(), cmd.baseVersion(), balance.version());
        } else {
            status = "PENDING";
        }

        // 5. Lưu dòng — kể cả khi CONFLICT để client có thể tra cứu lý do
        var line = new StocktakeLine(
                UUID.randomUUID(),
                sessionId,
                organizationId,
                storeId,
                balance.productId(),
                cmd.batchId(),
                cmd.clientOperationId(),
                balance.onHandQuantity(),   // expected = tồn hệ thống tại snapshot server
                cmd.actualQuantity(),
                cmd.baseVersion(),
                cmd.note() == null ? "" : cmd.note(),
                status,
                conflictReason,
                cmd.countedAt()
        );

        StocktakeLine saved = stocktakeRepo.saveLine(line);

        // 6. Ném exception SAU KHI đã lưu để client nhận 409 với body đủ thông tin
        if ("CONFLICT".equals(status)) {
            throw new StocktakeConflictException(saved);
        }

        return saved;
    }

    // ── Approve stocktake (INV-03) ───────────────────────────────────────────

    /**
     * Quản lý duyệt điều chỉnh tồn kho cho phiên kiểm kê.
     *
     * <p>Quy tắc nghiệp vụ:
     * <ul>
     *   <li>Chỉ MANAGER / ADMIN duyệt trong đúng store; không duyệt hai lần.</li>
     *   <li>Trong 1 transaction: kiểm tra version của balance khớp với baseVersion
     *       của số đếm; cập nhật balance = actualQuantity, ghi STOCKTAKE_ADJUSTMENT movement.</li>
     *   <li>Nếu version balance lệch hoặc có dòng CONFLICT: từ chối duyệt, không ghi một phần.</li>
     *   <li>Không âm tồn kho.</li>
     * </ul>
     */
    @Transactional
    public Stocktake approveStocktake(UUID organizationId, UUID storeId, UUID actorId, UUID sessionId) {
        Stocktake session = stocktakeRepo.findById(organizationId, storeId, sessionId)
                .orElseThrow(() -> new StocktakeException("NOT_FOUND",
                        "Phiên kiểm kê không tồn tại: " + sessionId));

        if (session.isApproved()) {
            throw new StocktakeException("ALREADY_APPROVED",
                    "Phiên kiểm kê đã được duyệt trước đó; không thể duyệt lại.");
        }

        if ("CANCELLED".equals(session.status())) {
            throw new StocktakeException("SESSION_CANCELLED",
                    "Phiên kiểm kê đã bị hủy; không thể duyệt.");
        }

        List<StocktakeLine> lines = stocktakeRepo.findLines(sessionId);
        if (lines.isEmpty()) {
            throw new StocktakeException("EMPTY_STOCKTAKE",
                    "Phiên kiểm kê không có dòng kiểm nào.");
        }

        boolean hasConflict = lines.stream().anyMatch(l -> "CONFLICT".equals(l.status()));
        if (hasConflict) {
            throw new StocktakeException("UNRESOLVED_CONFLICT",
                    "Phiên có dòng đang ở trạng thái CONFLICT; cần xử lý xung đột trước khi duyệt.");
        }

        Instant now = Instant.now();
        List<StocktakeLine> approvedLines = new ArrayList<>();

        for (StocktakeLine line : lines) {
            InventoryBalance balance = adjustmentPort.findAndLockBalance(organizationId, storeId, line.batchId())
                    .orElseThrow(() -> new StocktakeException("NOT_FOUND",
                            "Không tìm thấy số dư tồn kho cho lô: " + line.batchId()));

            if (balance.version() != line.baseVersion()) {
                throw new StocktakeException("BALANCE_VERSION_MISMATCH", String.format(
                        "Tồn kho lô %s đã thay đổi trước khi duyệt (baseVersion=%d, currentVersion=%d).",
                        line.batchId(), line.baseVersion(), balance.version()));
            }

            BigDecimal actualQty = line.actualQuantity();
            if (actualQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new StocktakeException("NEGATIVE_BALANCE",
                        "Số lượng thực tế không được âm: " + actualQty);
            }

            BigDecimal currentQty = balance.onHandQuantity();
            BigDecimal delta = actualQty.subtract(currentQty);

            // Cập nhật tồn kho mới
            adjustmentPort.updateBalance(organizationId, storeId, line.batchId(), actualQty);

            // Ghi biến động điều chỉnh kho nếu có chênh lệch
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                adjustmentPort.recordAdjustmentMovement(
                        organizationId, storeId, line.productId(), line.batchId(),
                        delta, sessionId, actorId, now);
            }

            // Đổi trạng thái dòng sang APPROVED
            stocktakeRepo.updateLineStatus(line.id(), "APPROVED", null);
            approvedLines.add(line.approve());
        }

        Instant submittedAt = session.submittedAt() != null ? session.submittedAt() : now;
        stocktakeRepo.updateSessionStatus(sessionId, "APPROVED", submittedAt);

        adjustmentPort.recordAuditLog(
                organizationId, actorId, "APPROVE_STOCKTAKE", "STOCKTAKE", sessionId,
                String.format("{\"linesCount\":%d}", lines.size()));

        return session.approve(submittedAt, approvedLines);
    }
}
