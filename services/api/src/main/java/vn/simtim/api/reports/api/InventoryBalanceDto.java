package vn.simtim.api.reports.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryBalanceDto(
        UUID productId,
        String sku,
        String name,
        BigDecimal quantity,
        LocalDate expiryDate,
        String status
) {
}
