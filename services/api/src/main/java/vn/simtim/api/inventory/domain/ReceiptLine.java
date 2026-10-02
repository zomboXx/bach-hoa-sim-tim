package vn.simtim.api.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Một dòng trong phiếu nhận hàng.
 * accepted + rejected = delivered; nếu rejected > 0 hoặc delivered != expected thì cần discrepancyReason.
 */
public record ReceiptLine(
        UUID id,
        UUID receiptId,
        UUID organizationId,
        UUID productId,
        BigDecimal expectedQuantity,
        BigDecimal deliveredQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        long unitCost,
        String supplierLotNumber,
        LocalDate expiryDate,
        String discrepancyReason) {

    public boolean hasDiscrepancy() {
        return rejectedQuantity.compareTo(BigDecimal.ZERO) > 0
                || deliveredQuantity.compareTo(expectedQuantity) != 0;
    }

    public boolean createsStock() {
        return acceptedQuantity.compareTo(BigDecimal.ZERO) > 0;
    }
}
