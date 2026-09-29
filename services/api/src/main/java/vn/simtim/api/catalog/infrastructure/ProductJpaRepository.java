package vn.simtim.api.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductJpa, UUID> {

    List<ProductJpa> findByOrganizationId(UUID organizationId);

    @Query("select p from ProductJpa p where p.organizationId = :orgId and p.id = :id")
    Optional<ProductJpa> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);

    @Query("select p from ProductJpa p where p.organizationId = :orgId and lower(p.sku) = lower(:sku)")
    Optional<ProductJpa> findBySkuInsensitive(@Param("orgId") UUID orgId, @Param("sku") String sku);

    @Query("select count(p) > 0 from ProductJpa p " +
           "where p.organizationId = :orgId and lower(p.sku) = lower(:sku)")
    boolean existsBySkuInsensitive(@Param("orgId") UUID orgId, @Param("sku") String sku);

    @Query("select count(p) > 0 from ProductJpa p " +
           "where p.organizationId = :orgId and lower(p.sku) = lower(:sku) and p.id <> :excludeId")
    boolean existsBySkuInsensitiveExcluding(
            @Param("orgId") UUID orgId, @Param("sku") String sku, @Param("excludeId") UUID excludeId);

    @Query("select p from ProductJpa p where p.organizationId = :orgId " +
           "and lower(p.name) like lower(concat('%', :keyword, '%'))")
    List<ProductJpa> searchByName(@Param("orgId") UUID orgId, @Param("keyword") String keyword);
}
