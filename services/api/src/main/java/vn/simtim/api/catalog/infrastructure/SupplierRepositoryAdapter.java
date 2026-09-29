package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.catalog.domain.Supplier;
import vn.simtim.api.catalog.domain.SupplierRepository;

@Repository
class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    SupplierRepositoryAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Supplier> findByOrganization(UUID organizationId) {
        return jpa.findByOrganizationId(organizationId).stream().map(SupplierJpa::toDomain).toList();
    }

    @Override
    public Optional<Supplier> findById(UUID organizationId, UUID id) {
        return jpa.findByOrganizationIdAndId(organizationId, id).map(SupplierJpa::toDomain);
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
    public Supplier save(Supplier supplier) {
        return jpa.save(new SupplierJpa(supplier)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.findByOrganizationIdAndId(organizationId, id)
                .ifPresent(e -> jpa.deleteById(e.id));
    }
}
