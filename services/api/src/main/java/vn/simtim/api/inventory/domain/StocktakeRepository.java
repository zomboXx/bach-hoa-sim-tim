package vn.simtim.api.inventory.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cổng domain cho phiên kiểm kê — không phụ thuộc JPA hay HTTP.
 */
public interface StocktakeRepository {

    /** Lưu phiên mới (chưa có dòng). */
    Stocktake saveSession(Stocktake session);

    /** Tìm phiên OPEN của actor trong store hiện tại. */
    Optional<Stocktake> findOpenSession(UUID organizationId, UUID storeId, UUID actorId);

    /** Tìm phiên bất kỳ theo id trong phạm vi store. */
    Optional<Stocktake> findById(UUID organizationId, UUID storeId, UUID id);

    /** Lưu một dòng kiểm kê mới (idempotent: dựa trên clientOperationId). */
    StocktakeLine saveLine(StocktakeLine line);

    /** Lấy tất cả dòng của phiên. */
    List<StocktakeLine> findLines(UUID stocktakeId);

    /** Tìm dòng theo clientOperationId trong phạm vi org/store. */
    Optional<StocktakeLine> findLineByClientOperationId(
            UUID organizationId, UUID storeId, UUID clientOperationId);

    /** Cập nhật trạng thái một dòng (CONFLICT, APPROVED). */
    void updateLineStatus(UUID lineId, String status, String conflictReason);

    /** Cập nhật trạng thái phiên (APPROVED, CANCELLED) và thời gian submittedAt. */
    void updateSessionStatus(UUID sessionId, String status, Instant submittedAt);
}
