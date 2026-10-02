package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Phân bổ invoice đã lưu mà inventory sẽ trừ và ghi movement. */
public record SaleIssue(
        UUID movementId,
        UUID invoiceLineId,
        UUID productId,
        UUID batchId,
        BigDecimal quantity,
        UUID actorId) {

    public SaleIssue {
        Objects.requireNonNull(movementId, "movementId");
        Objects.requireNonNull(invoiceLineId, "invoiceLineId");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(actorId, "actorId");
        if (quantity.signum() <= 0 || quantity.scale() > 3) {
            throw new IllegalArgumentException("quantity phải dương và có tối đa 3 chữ số lẻ");
        }
    }
}
