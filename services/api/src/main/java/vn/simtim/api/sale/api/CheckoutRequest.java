package vn.simtim.api.sale.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Body của POST /api/v1/invoices — tạo hoá đơn và trừ tồn nguyên tử. */
public record CheckoutRequest(
        @NotNull UUID storeId,
        @NotEmpty List<ItemRequest> items,
        @NotNull @Min(1) Long cashAmount) {

    public record ItemRequest(
            @NotNull UUID productId,
            @NotNull BigDecimal quantity) {}
}
