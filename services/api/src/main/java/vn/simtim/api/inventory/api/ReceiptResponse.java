package vn.simtim.api.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.inventory.domain.GoodsReceipt;
import vn.simtim.api.inventory.domain.ReceiptLine;

/** HTTP response cho phiếu nhận hàng. */
public record ReceiptResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID supplierId,
        String status,
        Instant receivedAt,
        UUID confirmedBy,
        UUID clientOperationId,
        List<LineResponse> lines) {

    public static ReceiptResponse from(GoodsReceipt r) {
        return new ReceiptResponse(
                r.id(), r.organizationId(), r.storeId(), r.supplierId(),
                r.status(), r.receivedAt(), r.confirmedBy(), r.clientOperationId(),
                r.lines().stream().map(LineResponse::from).toList());
    }

    public record LineResponse(
            UUID id,
            UUID productId,
            BigDecimal expectedQuantity,
            BigDecimal deliveredQuantity,
            BigDecimal acceptedQuantity,
            BigDecimal rejectedQuantity,
            long unitCost,
            String supplierLotNumber,
            LocalDate expiryDate,
            String discrepancyReason) {

        public static LineResponse from(ReceiptLine l) {
            return new LineResponse(l.id(), l.productId(),
                    l.expectedQuantity(), l.deliveredQuantity(),
                    l.acceptedQuantity(), l.rejectedQuantity(),
                    l.unitCost(), l.supplierLotNumber(),
                    l.expiryDate(), l.discrepancyReason());
        }
    }
}
