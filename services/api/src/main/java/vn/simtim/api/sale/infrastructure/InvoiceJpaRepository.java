package vn.simtim.api.sale.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceJpaRepository extends JpaRepository<InvoiceJpa, UUID> {

    @Query("SELECT i FROM InvoiceJpa i WHERE i.organizationId = :orgId AND i.id = :id")
    Optional<InvoiceJpa> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);

    @Query("SELECT i FROM InvoiceJpa i WHERE i.organizationId = :orgId AND i.storeId = :storeId ORDER BY i.soldAt DESC")
    List<InvoiceJpa> findByOrganizationIdAndStoreId(@Param("orgId") UUID orgId, @Param("storeId") UUID storeId);
}
