package vn.simtim.api.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductImageJpaRepository extends JpaRepository<ProductImageJpa, UUID> {

    Optional<ProductImageJpa> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
