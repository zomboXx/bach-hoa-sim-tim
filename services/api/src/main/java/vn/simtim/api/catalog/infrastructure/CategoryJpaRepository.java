package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CategoryJpaRepository extends JpaRepository<CategoryJpa, UUID> {

    List<CategoryJpa> findByOrganizationId(UUID organizationId);

    @Query("select c from CategoryJpa c where c.organizationId = :orgId and c.id = :id")
    Optional<CategoryJpa> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);

    @Query("select count(c) > 0 from CategoryJpa c " +
           "where c.organizationId = :orgId and lower(c.code) = lower(:code)")
    boolean existsByCodeInsensitive(@Param("orgId") UUID orgId, @Param("code") String code);

    @Query("select count(c) > 0 from CategoryJpa c " +
           "where c.organizationId = :orgId and lower(c.code) = lower(:code) and c.id <> :excludeId")
    boolean existsByCodeInsensitiveExcluding(
            @Param("orgId") UUID orgId, @Param("code") String code, @Param("excludeId") UUID excludeId);
}
