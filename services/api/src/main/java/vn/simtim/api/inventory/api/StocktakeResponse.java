package vn.simtim.api.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.inventory.domain.Stocktake;
import vn.simtim.api.inventory.domain.StocktakeLine;

/** Response DTO cho phiên kiểm kê và dòng số đếm. */
public record StocktakeResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID actorId,
        String status,
        Instant openedAt,
        Instant submittedAt,
        List<LineDto> lines
) {

    public record LineDto(
            UUID id,
            UUID stocktakeId,
            UUID productId,
            UUID batchId,
            UUID clientOperationId,
            BigDecimal expectedQuantity,
            BigDecimal actualQuantity,
            long baseVersion,
            String note,
            String status,
            String conflictReason,
            Instant countedAt
    ) {
        static LineDto from(StocktakeLine l) {
            return new LineDto(l.id(), l.stocktakeId(), l.productId(), l.batchId(),
                    l.clientOperationId(), l.expectedQuantity(), l.actualQuantity(),
                    l.baseVersion(), l.note(), l.status(), l.conflictReason(), l.countedAt());
        }
    }

    public static StocktakeResponse from(Stocktake s) {
        return new StocktakeResponse(
                s.id(), s.organizationId(), s.storeId(), s.actorId(),
                s.status(), s.openedAt(), s.submittedAt(),
                s.lines().stream().map(LineDto::from).toList());
    }

    public static StocktakeResponse from(Stocktake s, List<StocktakeLine> lines) {
        return new StocktakeResponse(
                s.id(), s.organizationId(), s.storeId(), s.actorId(),
                s.status(), s.openedAt(), s.submittedAt(),
                lines.stream().map(LineDto::from).toList());
    }
}
