package vn.simtim.api.catalog.api;

import jakarta.validation.constraints.*;

/** Request body chung cho tạo/cập nhật đơn vị tính. */
public record UnitRequest(
        @NotBlank(message = "code không được trống")
        @Size(max = 20, message = "code tối đa 20 ký tự")
        String code,

        @NotBlank(message = "name không được trống")
        @Size(max = 50, message = "name tối đa 50 ký tự")
        String name,

        short precisionScale) {}
