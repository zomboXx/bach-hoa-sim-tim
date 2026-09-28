package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
    List<Unit> findByOrganization(UUID organizationId);
    Optional<Unit> findById(UUID organizationId, UUID id);
    boolean existsByCode(UUID organizationId, String code);
    boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId);
    Unit save(Unit unit);
    void deleteById(UUID organizationId, UUID id);
}
