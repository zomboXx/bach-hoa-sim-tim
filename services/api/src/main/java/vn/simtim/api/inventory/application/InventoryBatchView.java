package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Tồn hiện tại theo lô; không chứa giá vốn. */
public record InventoryBatchView(
        UUID batchId,
        UUID productId,
        String batchNumber,
        String supplierLotNumber,
        String batchStatus,
        ExpiryStatus expiryStatus,
        LocalDate expiryDate,
        LocalDate receivedDate,
        BigDecimal onHandQuantity,
        BigDecimal availableQuantity,
        LocalDate businessDate) {}
