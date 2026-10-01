package vn.simtim.api.inventory.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HTTP request body cho POST /api/v1/inventory/receipts.
 * Tuân theo shape trong contracts/SPRINT_2_BOUNDARY_DRAFT.md.
 */
public record ReceiptRequest(
        @NotNull UUID supplierId,
        @NotNull UUID clientOperationId,
        @NotNull @Size(min = 1, max = 100) List<LineRequest> lines) {

    public record LineRequest(
            @NotNull UUID productId,
            @NotNull @DecimalMin("0.001") BigDecimal expectedQuantity,
            @NotNull @DecimalMin("0") BigDecimal deliveredQuantity,
            @NotNull @DecimalMin("0") BigDecimal acceptedQuantity,
            @NotNull @DecimalMin("0") BigDecimal rejectedQuantity,
            @Min(0) long unitCost,
            String supplierLotNumber,
            LocalDate expiryDate,
            String discrepancyReason) {
    }
}
