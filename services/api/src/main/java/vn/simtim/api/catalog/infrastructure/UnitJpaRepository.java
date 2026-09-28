package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UnitJpaRepository extends JpaRepository<UnitJpa, UUID> {

    List<UnitJpa> findByOrganizationId(UUID organizationId);

    @Query("select count(u) > 0 from UnitJpa u " +
           "where u.organizationId = :orgId and lower(u.code) = lower(:code)")
    boolean existsByCodeInsensitive(@Param("orgId") UUID orgId, @Param("code") String code);
}
