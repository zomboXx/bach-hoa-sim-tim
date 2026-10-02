package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite PK (invoice_line_id, product_batch_id) cho bảng sales.invoice_line_batches. */
@Embeddable
class InvoiceLineBatchId implements Serializable {

    @Column(name = "invoice_line_id", nullable = false) UUID invoiceLineId;
    @Column(name = "product_batch_id", nullable = false) UUID productBatchId;

    InvoiceLineBatchId() {}

    InvoiceLineBatchId(UUID invoiceLineId, UUID productBatchId) {
        this.invoiceLineId = invoiceLineId;
        this.productBatchId = productBatchId;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceLineBatchId that)) return false;
        return Objects.equals(invoiceLineId, that.invoiceLineId) &&
               Objects.equals(productBatchId, that.productBatchId);
    }

    @Override public int hashCode() { return Objects.hash(invoiceLineId, productBatchId); }
}
