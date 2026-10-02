package vn.simtim.api.inventory.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Dòng sổ biến động kèm chứng từ nguồn. */
public record InventoryMovementView(
        UUID movementId,
        UUID productId,
        UUID batchId,
        String movementType,
        BigDecimal quantityDelta,
        String sourceType,
        UUID sourceId,
        Instant occurredAt,
        UUID recordedBy) {}
