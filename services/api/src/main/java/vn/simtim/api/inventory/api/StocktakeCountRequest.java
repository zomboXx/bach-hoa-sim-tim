package vn.simtim.api.inventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Body POST /api/v1/inventory/stocktakes/{sessionId}/counts
 */
public record StocktakeCountRequest(

        @NotNull UUID clientOperationId,

        @NotNull UUID batchId,

        @NotNull
        @DecimalMin(value = "0", message = "actualQuantity phải >= 0")
        BigDecimal actualQuantity,

        long baseVersion,

        String note,

        Instant countedAt
) {}
