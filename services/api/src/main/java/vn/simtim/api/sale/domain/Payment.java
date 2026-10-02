package vn.simtim.api.sale.domain;

import java.time.Instant;
import java.util.UUID;

/** Thanh toán cho hóa đơn. SAL-01 chỉ hỗ trợ CASH/COMPLETED ngay lập tức. */
public record Payment(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID invoiceId,
        String method,
        String status,
        long amount,
        Instant paidAt) {}
