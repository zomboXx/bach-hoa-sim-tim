package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import vn.simtim.api.sale.domain.Payment;

@Entity
@Table(schema = "sales", name = "payments")
class PaymentJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "invoice_id", nullable = false) UUID invoiceId;
    @Column(nullable = false, length = 24) String method;
    @Column(nullable = false, length = 16) String status;
    @Column(nullable = false) long amount;
    @Column(name = "reference_code", length = 100) String referenceCode;
    @Column(name = "paid_at") Instant paidAt;

    PaymentJpa() {}

    PaymentJpa(Payment p) {
        this.id = p.id();
        this.organizationId = p.organizationId();
        this.storeId = p.storeId();
        this.invoiceId = p.invoiceId();
        this.method = p.method();
        this.status = p.status();
        this.amount = p.amount();
        this.paidAt = p.paidAt();
    }

    Payment toDomain() {
        return new Payment(id, organizationId, storeId, invoiceId, method, status, amount, paidAt);
    }
}
