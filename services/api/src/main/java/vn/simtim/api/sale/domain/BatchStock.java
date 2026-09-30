package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model dùng để chọn lô FEFO.
 * Chứa thông tin lô + số lượng tồn hiện tại.
 */
public record BatchStock(
        UUID batchId,
        UUID productId,
        BigDecimal quantityOnHand,
        LocalDate expiryDate) {}
