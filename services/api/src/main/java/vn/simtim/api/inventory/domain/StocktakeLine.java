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
) {}
