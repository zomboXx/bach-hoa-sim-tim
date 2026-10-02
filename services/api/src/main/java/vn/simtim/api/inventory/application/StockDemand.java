package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Nhu cầu xuất bán của một sản phẩm. */
public record StockDemand(UUID productId, BigDecimal quantity) {
    public StockDemand {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.signum() <= 0 || quantity.scale() > 3) {
            throw new IllegalArgumentException("quantity phải dương và có tối đa 3 chữ số lẻ");
        }
    }
}
