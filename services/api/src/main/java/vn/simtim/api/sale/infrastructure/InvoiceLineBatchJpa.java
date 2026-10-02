package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import vn.simtim.api.sale.domain.InvoiceLineBatch;

@Entity
@Table(schema = "sales", name = "invoice_line_batches")
class InvoiceLineBatchJpa {

    @EmbeddedId InvoiceLineBatchId pk;

    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal quantity;

    InvoiceLineBatchJpa() {}

    InvoiceLineBatchJpa(InvoiceLineBatch b) {
        this.pk = new InvoiceLineBatchId(b.invoiceLineId(), b.productBatchId());
        this.organizationId = b.organizationId();
        this.storeId = b.storeId();
        this.productId = b.productId();
        this.quantity = b.quantity();
    }

    InvoiceLineBatch toDomain() {
        return new InvoiceLineBatch(organizationId, storeId, pk.invoiceLineId,
                productId, pk.productBatchId, quantity);
    }
}
