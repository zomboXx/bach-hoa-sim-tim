package vn.simtim.api.catalog.domain;

import java.util.UUID;

public record Supplier(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String phone,
        String email,
        String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
