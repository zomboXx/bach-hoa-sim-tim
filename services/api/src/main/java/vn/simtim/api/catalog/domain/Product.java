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
        long version,
        String imageUrl) {

    public Product(UUID id, UUID organizationId, UUID categoryId, UUID baseUnitId,
                   String sku, String name, boolean tracksExpiry, String status, long version) {
        this(id, organizationId, categoryId, baseUnitId, sku, name, tracksExpiry, status, version, null);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
