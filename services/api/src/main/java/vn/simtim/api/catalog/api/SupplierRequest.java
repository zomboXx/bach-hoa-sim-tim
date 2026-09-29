package vn.simtim.api.catalog.api;

import jakarta.validation.constraints.*;

/** Request body chung cho tạo/cập nhật nhà cung cấp. */
public record SupplierRequest(
        @NotBlank(message = "code không được trống")
        @Size(max = 40, message = "code tối đa 40 ký tự")
        String code,

        @NotBlank(message = "name không được trống")
        @Size(max = 200, message = "name tối đa 200 ký tự")
        String name,

        @Size(max = 40, message = "phone tối đa 40 ký tự")
        String phone,

        @Email(message = "email không hợp lệ")
        @Size(max = 255, message = "email tối đa 255 ký tự")
        String email,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "status phải là ACTIVE hoặc INACTIVE")
        String status) {}
