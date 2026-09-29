package vn.simtim.api.catalog.api;

import java.util.UUID;
import vn.simtim.api.catalog.domain.Supplier;

/** Response body cho nhà cung cấp. */
public record SupplierResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String phone,
        String email,
        String status) {

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.id(), s.organizationId(),
                s.code(), s.name(), s.phone(), s.email(), s.status());
    }
}
