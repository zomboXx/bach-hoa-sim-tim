package vn.simtim.api.catalog.domain;

import java.util.UUID;

public record Category(
        UUID id,
        UUID organizationId,
        UUID parentId,
        String code,
        String name,
        String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
