package vn.simtim.api.catalog.api;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Request body chung cho tạo/cập nhật danh mục. */
public record CategoryRequest(
        UUID parentId,

        @NotBlank(message = "code không được trống")
        @Size(max = 40, message = "code tối đa 40 ký tự")
        String code,

        @NotBlank(message = "name không được trống")
        @Size(max = 200, message = "name tối đa 200 ký tự")
        String name,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "status phải là ACTIVE hoặc INACTIVE")
        String status) {}
