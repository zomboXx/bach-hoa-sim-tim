package vn.simtim.api.catalog.api;

import jakarta.validation.constraints.*;

/** Request body chung cho tạo/cập nhật sản phẩm. */
public record ProductRequest(
        @NotNull(message = "categoryId không được null")
        java.util.UUID categoryId,

        @NotNull(message = "baseUnitId không được null")
        java.util.UUID baseUnitId,

        @NotBlank(message = "sku không được trống")
        @Size(max = 80, message = "sku tối đa 80 ký tự")
        String sku,

        @NotBlank(message = "name không được trống")
        @Size(max = 200, message = "name tối đa 200 ký tự")
        String name,

        boolean tracksExpiry,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "status phải là ACTIVE hoặc INACTIVE")
        String status) {}
