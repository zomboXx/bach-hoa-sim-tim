package vn.simtim.api.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phiên kiểm kê của một nhân viên tại một cửa hàng.
 * Trạng thái: OPEN → SUBMITTED → APPROVED / CANCELLED
 */
public record Stocktake(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID actorId,
        String status,
        Instant openedAt,
        Instant submittedAt,
        List<StocktakeLine> lines
) {
    /** Phiên mới chỉ có thể OPEN. */
    public static Stocktake open(UUID organizationId, UUID storeId, UUID actorId) {
        return new Stocktake(UUID.randomUUID(), organizationId, storeId, actorId,
                "OPEN", Instant.now(), null, List.of());
    }

    public boolean isOpen() { return "OPEN".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }

    public Stocktake approve(Instant submittedAt, List<StocktakeLine> approvedLines) {
        return new Stocktake(id, organizationId, storeId, actorId,
                "APPROVED", openedAt, submittedAt != null ? submittedAt : Instant.now(), approvedLines);
    }
}
