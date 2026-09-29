package vn.simtim.api.catalog.api;

import java.util.UUID;
import vn.simtim.api.catalog.domain.Category;

/** Response body cho danh mục. */
public record CategoryResponse(
        UUID id,
        UUID organizationId,
        UUID parentId,
        String code,
        String name,
        String status) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.organizationId(), c.parentId(),
                c.code(), c.name(), c.status());
    }
}
