package vn.simtim.api.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng số đếm trong phiên kiểm kê — theo từng lô.
 */
public record StocktakeLine(
        UUID id,
        UUID stocktakeId,
        UUID organizationId,
        UUID storeId,
        UUID productId,
        UUID batchId,
        UUID clientOperationId,
        BigDecimal expectedQuantity,
        BigDecimal actualQuantity,
        long baseVersion,
        String note,
        String status,          // PENDING | CONFLICT | APPROVED
        String conflictReason,
        Instant countedAt
) {
    public StocktakeLine approve() {
        return new StocktakeLine(id, stocktakeId, organizationId, storeId, productId,
                batchId, clientOperationId, expectedQuantity, actualQuantity,
                baseVersion, note, "APPROVED", null, countedAt);
    }

    public StocktakeLine conflict(String reason) {
        return new StocktakeLine(id, stocktakeId, organizationId, storeId, productId,
                batchId, clientOperationId, expectedQuantity, actualQuantity,
                baseVersion, note, "CONFLICT", reason, countedAt);
    }
}
