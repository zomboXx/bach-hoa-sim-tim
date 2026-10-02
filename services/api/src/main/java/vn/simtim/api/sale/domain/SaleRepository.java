package vn.simtim.api.sale.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port repository cho module sale.
 * Application service gọi interface này; infrastructure adapts nó.
 */
public interface SaleRepository {

    // --- Price and catalog lookups (read-only) ---

    /** Giá bán hiện hành tại thời điểm {@code at}, null nếu không có. */
    Optional<Long> findCurrentPrice(UUID orgId, UUID storeId, UUID productId, Instant at);

    /** Snapshot SKU và tên sản phẩm để ghi vào invoice_line. */
    Optional<ProductSnapshot> findProductSnapshot(UUID orgId, UUID productId);

    // --- Write operations (trong transaction của SaleService) ---

    Invoice saveInvoice(Invoice invoice);

    InvoiceLine saveInvoiceLine(InvoiceLine line);

    InvoiceLineBatch saveInvoiceLineBatch(InvoiceLineBatch lineBatch);

    Payment savePayment(Payment payment);

    // --- Read operations ---

    Optional<Invoice> findInvoiceById(UUID orgId, UUID id);

    List<Invoice> listInvoices(UUID orgId, UUID storeId);

    List<InvoiceLine> findLinesByInvoiceId(UUID orgId, UUID invoiceId);

    List<InvoiceLineBatch> findLineBatchesByInvoiceLineId(UUID orgId, UUID invoiceLineId);

    List<Payment> findPaymentsByInvoiceId(UUID orgId, UUID invoiceId);
}
