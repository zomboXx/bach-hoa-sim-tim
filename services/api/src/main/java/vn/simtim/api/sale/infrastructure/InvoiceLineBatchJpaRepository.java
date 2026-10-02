package vn.simtim.api.sale.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceLineBatchJpaRepository extends JpaRepository<InvoiceLineBatchJpa, InvoiceLineBatchId> {

    @Query("SELECT b FROM InvoiceLineBatchJpa b WHERE b.organizationId = :orgId AND b.pk.invoiceLineId = :lineId")
    List<InvoiceLineBatchJpa> findByOrganizationIdAndInvoiceLineId(@Param("orgId") UUID orgId,
                                                                    @Param("lineId") UUID lineId);
}
