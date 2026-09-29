package vn.simtim.api.catalog.api;

import java.util.UUID;
import vn.simtim.api.catalog.domain.ProductBarcode;

public record ProductBarcodeResponse(
        UUID id,
        String barcode,
        boolean isPrimary) {
        
    public static ProductBarcodeResponse from(ProductBarcode b) {
        return new ProductBarcodeResponse(b.id(), b.barcode(), b.isPrimary());
    }
}
