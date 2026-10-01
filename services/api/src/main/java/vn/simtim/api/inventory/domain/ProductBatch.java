package vn.simtim.api.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * Một lô hàng vật lý được tạo khi acceptedQuantity > 0 trong một dòng phiếu nhận.
 */
public record ProductBatch(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID productId,
        UUID receiptLineId,
        String batchNumber,
        String supplierLotNumber,
        LocalDate expiryDate,
        LocalDate receivedDate,
        String status) {

    /** Lô còn hạn nếu expiryDate là null hoặc không trước businessDate. */
    public boolean isAvailableOn(LocalDate businessDate) {
        if (!"AVAILABLE".equals(status)) return false;
        if (expiryDate == null) return true;
        return !expiryDate.isBefore(businessDate);
    }
}
