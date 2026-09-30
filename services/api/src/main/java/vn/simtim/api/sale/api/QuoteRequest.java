package vn.simtim.api.sale.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Body của POST /api/v1/invoices/quote — chỉ tính giá, không ghi DB. */
public record QuoteRequest(
        @NotNull UUID storeId,
        @NotEmpty List<ItemRequest> items) {

    public record ItemRequest(
            @NotNull UUID productId,
            @NotNull BigDecimal quantity) {}
}
