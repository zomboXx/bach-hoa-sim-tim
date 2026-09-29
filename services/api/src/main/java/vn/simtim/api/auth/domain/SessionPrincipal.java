package vn.simtim.api.auth.domain;

import java.util.Set;
import java.util.UUID;

public record SessionPrincipal(UUID userId, UUID organizationId, UUID storeId, String fullName,
        boolean trainingEnabled, Set<String> roles, Set<String> permissions) {
    public SessionPrincipal {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
