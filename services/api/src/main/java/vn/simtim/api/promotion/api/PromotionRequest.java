package vn.simtim.api.promotion.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request body dùng chung cho tạo/cập nhật khuyến mãi.
 * storeId không còn là trường của request — lấy từ SessionPrincipal.
 */
public record PromotionRequest(
        @NotBlank(message = "code không được trống")
        @Size(max = 60, message = "code tối đa 60 ký tự")
        String code,

        @NotBlank(message = "name không được trống")
        @Size(max = 200, message = "name tối đa 200 ký tự")
        String name,

        @NotNull(message = "discountType không được null")
        @Pattern(regexp = "AMOUNT|PERCENT", message = "discountType phải là AMOUNT hoặc PERCENT")
        String discountType,

        @NotNull(message = "discountValue không được null")
        @DecimalMin(value = "0", inclusive = false, message = "discountValue phải lớn hơn 0")
        BigDecimal discountValue,

        @NotNull(message = "startsAt không được null")
        Instant startsAt,

        @NotNull(message = "endsAt không được null")
        Instant endsAt,

        @Pattern(regexp = "DRAFT|ACTIVE|INACTIVE", message = "status phải là DRAFT, ACTIVE hoặc INACTIVE")
        String status) {}
