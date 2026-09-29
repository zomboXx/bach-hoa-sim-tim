package vn.simtim.api.catalog.domain;

import java.util.UUID;

public record Product(
        UUID id,
        UUID organizationId,
        UUID categoryId,
        UUID baseUnitId,
        String sku,
        String name,
        boolean tracksExpiry,
        String status,
        long version) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
