package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import vn.simtim.api.sale.domain.Invoice;

@Entity
@Table(schema = "sales", name = "invoices")
class InvoiceJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "invoice_no", nullable = false, length = 60) String invoiceNo;
    @Column(nullable = false, length = 16) String status;
    @Column(name = "sold_by", nullable = false) UUID soldBy;
    @Column(name = "sold_at", nullable = false) Instant soldAt;
    @Column(nullable = false) long subtotal;
    @Column(name = "discount_total", nullable = false) long discountTotal;
    @Column(name = "grand_total", nullable = false) long grandTotal;
    @Column(name = "paid_total", nullable = false) long paidTotal;
    @Column(name = "voided_by") UUID voidedBy;
    @Column(name = "void_reason", length = 500) String voidReason;
    @Version @Column(nullable = false) long version;

    InvoiceJpa() {}

    InvoiceJpa(Invoice inv) {
        this.id = inv.id();
        this.organizationId = inv.organizationId();
        this.storeId = inv.storeId();
        this.invoiceNo = inv.invoiceNo();
        this.status = inv.status();
        this.soldBy = inv.soldBy();
        this.soldAt = inv.soldAt();
        this.subtotal = inv.subtotal();
        this.discountTotal = inv.discountTotal();
        this.grandTotal = inv.grandTotal();
        this.paidTotal = inv.paidTotal();
        this.version = inv.version();
    }

    Invoice toDomain() {
        return new Invoice(id, organizationId, storeId, invoiceNo, status,
                soldBy, soldAt, subtotal, discountTotal, grandTotal, paidTotal,
                java.util.List.of(), java.util.List.of(), version);
    }
}
