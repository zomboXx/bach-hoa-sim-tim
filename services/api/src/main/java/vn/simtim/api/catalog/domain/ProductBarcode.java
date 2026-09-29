package vn.simtim.api.catalog.domain;

import java.util.UUID;

public record ProductBarcode(
        UUID id,
        UUID organizationId,
        UUID productId,
        String barcode,
        boolean isPrimary) {}
