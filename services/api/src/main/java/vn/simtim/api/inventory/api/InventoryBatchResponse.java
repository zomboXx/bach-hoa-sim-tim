package vn.simtim.api.inventory.api;

import java.time.LocalDate;
import java.util.UUID;
import vn.simtim.api.inventory.application.InventoryBatchView;

public record InventoryBatchResponse(
        UUID batchId,
        UUID productId,
        String batchNumber,
        String supplierLotNumber,
        String status,
        String expiryStatus,
        LocalDate expiryDate,
        LocalDate receivedDate,
        String onHandQuantity,
        String availableQuantity,
        LocalDate businessDate) {

    static InventoryBatchResponse from(InventoryBatchView view) {
        return new InventoryBatchResponse(
                view.batchId(), view.productId(), view.batchNumber(), view.supplierLotNumber(),
                view.batchStatus(), view.expiryStatus().name(), view.expiryDate(), view.receivedDate(),
                view.onHandQuantity().toPlainString(), view.availableQuantity().toPlainString(),
                view.businessDate());
    }
}
