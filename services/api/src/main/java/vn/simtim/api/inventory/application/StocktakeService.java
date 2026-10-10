package vn.simtim.api.inventory.application;

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

    public StocktakeService(StocktakeRepository stocktakeRepo,
                            InventoryBalanceReader balanceReader) {
        this.stocktakeRepo = stocktakeRepo;
        this.balanceReader  = balanceReader;
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
}
