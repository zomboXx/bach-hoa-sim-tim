package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Tổng tồn hiện tại của một sản phẩm trong cửa hàng. */
public record InventoryProductView(
        UUID productId,
        String sku,
        String productName,
        String productStatus,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        LocalDate businessDate) {}
