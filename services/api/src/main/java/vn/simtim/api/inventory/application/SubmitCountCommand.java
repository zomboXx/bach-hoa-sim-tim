package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lệnh gửi số đếm thực tế cho một lô trong phiên kiểm kê.
 * Đến từ HTTP layer; không phụ thuộc HTTP.
 */
public record SubmitCountCommand(
        UUID clientOperationId,
        UUID batchId,
        BigDecimal actualQuantity,
        long baseVersion,
        String note,
        Instant countedAt
) {}
