package vn.simtim.api.inventory.api;

import java.time.Instant;
import java.util.UUID;
import vn.simtim.api.inventory.application.InventoryMovementView;

public record InventoryMovementResponse(
        UUID movementId,
        UUID productId,
        UUID batchId,
        String type,
        String quantityDelta,
        MovementSourceResponse source,
        Instant occurredAt,
        UUID recordedBy) {

    public record MovementSourceResponse(String type, UUID id) {}

    static InventoryMovementResponse from(InventoryMovementView view) {
        return new InventoryMovementResponse(
                view.movementId(), view.productId(), view.batchId(), view.movementType(),
                view.quantityDelta().toPlainString(),
                new MovementSourceResponse(view.sourceType(), view.sourceId()),
                view.occurredAt(), view.recordedBy());
    }
}
