package vn.simtim.api.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductBarcodeRequest(
        @NotBlank(message = "barcode không được trống")
        @Size(max = 100, message = "barcode tối đa 100 ký tự")
        String barcode,

        boolean isPrimary) {}
