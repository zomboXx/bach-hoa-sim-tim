package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command object cho use-case xác nhận phiếu nhận hàng.
 * payloadJson dùng để tính hash idempotency.
 */
public record ConfirmReceiptCommand(
        UUID supplierId,
        UUID clientOperationId,
        List<LineCmd> lines,
        String payloadJson) {

    public ConfirmReceiptCommand {
        lines = List.copyOf(lines);
    }

    public record LineCmd(
            UUID productId,
            BigDecimal expectedQuantity,
            BigDecimal deliveredQuantity,
            BigDecimal acceptedQuantity,
            BigDecimal rejectedQuantity,
            long unitCost,
            String supplierLotNumber,
            LocalDate expiryDate,
            String discrepancyReason) {
    }
}
