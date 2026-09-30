package vn.simtim.api.sale.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một sản phẩm trong yêu cầu quote/checkout.
 *
 * @param productId UUID sản phẩm
 * @param quantity  Số lượng (> 0, tối đa 3 chữ số thập phân)
 */
public record CheckoutItem(UUID productId, BigDecimal quantity) {}
