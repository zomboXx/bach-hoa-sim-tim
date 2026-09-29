package vn.simtim.api.catalog.api;

import java.util.UUID;
import vn.simtim.api.catalog.domain.Product;

/** Response body cho sản phẩm. */
public record ProductResponse(
        UUID id,
        UUID organizationId,
        UUID categoryId,
        UUID baseUnitId,
        String sku,
        String name,
        boolean tracksExpiry,
        String status,
        long version) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.organizationId(), p.categoryId(), p.baseUnitId(),
                p.sku(), p.name(), p.tracksExpiry(), p.status(), p.version());
    }
}
