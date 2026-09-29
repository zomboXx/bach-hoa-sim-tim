package vn.simtim.api.catalog.api;

import java.util.UUID;
import vn.simtim.api.catalog.domain.Unit;

/** Response body cho đơn vị tính. */
public record UnitResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        short precisionScale) {

    public static UnitResponse from(Unit u) {
        return new UnitResponse(u.id(), u.organizationId(), u.code(), u.name(), u.precisionScale());
    }
}
