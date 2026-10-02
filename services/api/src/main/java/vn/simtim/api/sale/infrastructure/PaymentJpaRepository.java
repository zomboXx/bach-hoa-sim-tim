package vn.simtim.api.sale.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentJpaRepository extends JpaRepository<PaymentJpa, UUID> {

    @Query("SELECT p FROM PaymentJpa p WHERE p.organizationId = :orgId AND p.invoiceId = :invoiceId")
    List<PaymentJpa> findByOrganizationIdAndInvoiceId(@Param("orgId") UUID orgId,
                                                      @Param("invoiceId") UUID invoiceId);
}
