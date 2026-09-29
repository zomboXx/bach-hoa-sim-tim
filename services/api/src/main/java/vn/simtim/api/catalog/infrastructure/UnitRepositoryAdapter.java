package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.Unit;
import vn.simtim.api.catalog.domain.UnitRepository;

@Repository
class UnitRepositoryAdapter implements UnitRepository {

    private final UnitJpaRepository jpa;

    UnitRepositoryAdapter(UnitJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Unit> findByOrganization(UUID organizationId) {
        return jpa.findByOrganizationId(organizationId).stream().map(UnitJpa::toDomain).toList();
    }

    @Override
    public Optional<Unit> findById(UUID organizationId, UUID id) {
        return jpa.findById(id).filter(u -> organizationId.equals(u.organizationId)).map(UnitJpa::toDomain);
    }

    @Override
    public boolean existsByCode(UUID organizationId, String code) {
        return jpa.existsByCodeInsensitive(organizationId, code);
    }

    @Override
    public boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId) {
        return jpa.existsByCodeInsensitiveExcluding(organizationId, code, excludeId);
    }

    @Override
    public Unit save(Unit unit) {
        return jpa.save(new UnitJpa(unit)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.deleteByOrganizationIdAndId(organizationId, id);
    }
}
