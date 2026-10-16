package vn.simtim.api.inventory.api;

import java.time.LocalDate;
import java.util.UUID;
import vn.simtim.api.inventory.application.InventoryProductView;

public record InventoryProductResponse(
        UUID productId,
        String sku,
        String productName,
        String unitCode,
        String productStatus,
        String onHandQuantity,
        String availableQuantity,
        LocalDate businessDate) {

    static InventoryProductResponse from(InventoryProductView view) {
        return new InventoryProductResponse(
                view.productId(), view.sku(), view.productName(), view.unitCode(), view.productStatus(),
                view.onHandQuantity().toPlainString(), view.availableQuantity().toPlainString(),
                view.businessDate());
    }
}
