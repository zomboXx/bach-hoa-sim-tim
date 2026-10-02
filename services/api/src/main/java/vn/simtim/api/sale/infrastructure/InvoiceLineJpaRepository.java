package vn.simtim.api.sale.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceLineJpaRepository extends JpaRepository<InvoiceLineJpa, UUID> {

    @Query("SELECT l FROM InvoiceLineJpa l WHERE l.organizationId = :orgId AND l.invoiceId = :invoiceId")
    List<InvoiceLineJpa> findByOrganizationIdAndInvoiceId(@Param("orgId") UUID orgId,
                                                          @Param("invoiceId") UUID invoiceId);
}
