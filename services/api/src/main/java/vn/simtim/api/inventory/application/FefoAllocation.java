package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Phần phân bổ FEFO công khai; không làm lộ JPA entity hay giá vốn. */
public record FefoAllocation(
        UUID productId,
        UUID batchId,
        BigDecimal quantity,
        LocalDate expiryDate,
        LocalDate receivedDate) {}
