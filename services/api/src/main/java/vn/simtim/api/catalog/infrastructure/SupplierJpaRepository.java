package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SupplierJpaRepository extends JpaRepository<SupplierJpa, UUID> {

    List<SupplierJpa> findByOrganizationId(UUID organizationId);

    @Query("select s from SupplierJpa s where s.organizationId = :orgId and s.id = :id")
    Optional<SupplierJpa> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);

    @Query("select count(s) > 0 from SupplierJpa s " +
           "where s.organizationId = :orgId and lower(s.code) = lower(:code)")
    boolean existsByCodeInsensitive(@Param("orgId") UUID orgId, @Param("code") String code);

    @Query("select count(s) > 0 from SupplierJpa s " +
           "where s.organizationId = :orgId and lower(s.code) = lower(:code) and s.id <> :excludeId")
    boolean existsByCodeInsensitiveExcluding(
            @Param("orgId") UUID orgId, @Param("code") String code, @Param("excludeId") UUID excludeId);
}
