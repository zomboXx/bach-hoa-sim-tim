package vn.simtim.api.inventory.application;

import java.util.Objects;
import java.util.UUID;

/** Phạm vi organization/store lấy từ session, không lấy từ request body. */
public record StoreScope(UUID organizationId, UUID storeId) {
    public StoreScope {
        Objects.requireNonNull(organizationId, "organizationId");
        Objects.requireNonNull(storeId, "storeId");
    }
}
