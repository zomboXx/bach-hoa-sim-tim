package vn.simtim.api.inventory.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phiếu nhận hàng đã xác nhận (CONFIRMED). Bất biến sau khi tạo.
 */
public record GoodsReceipt(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID supplierId,
        String status,
        Instant receivedAt,
        UUID confirmedBy,
        UUID clientOperationId,
        List<ReceiptLine> lines) {

    public GoodsReceipt {
        lines = List.copyOf(lines);
    }
}
