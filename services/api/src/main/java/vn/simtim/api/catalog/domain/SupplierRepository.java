package vn.simtim.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository {
    List<Supplier> findByOrganization(UUID organizationId);
    Optional<Supplier> findById(UUID organizationId, UUID id);
    boolean existsByCode(UUID organizationId, String code);
    boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId);
    Supplier save(Supplier supplier);
    void deleteById(UUID organizationId, UUID id);
}
