package vn.simtim.api.catalog.domain;

import java.util.UUID;

public record Unit(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        short precisionScale) {}
