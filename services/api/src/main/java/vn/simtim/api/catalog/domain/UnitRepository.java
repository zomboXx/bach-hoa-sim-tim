package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
    List<Unit> findByOrganization(UUID organizationId);
    Optional<Unit> findById(UUID organizationId, UUID id);
    boolean existsByCode(UUID organizationId, String code);
    Unit save(Unit unit);
}
