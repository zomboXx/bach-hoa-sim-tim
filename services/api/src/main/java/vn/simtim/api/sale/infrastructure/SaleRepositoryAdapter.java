package vn.simtim.api.sale.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.sale.domain.*;

/**
 * Infrastructure adapter cho module sale: dịch giữa domain port và JPA/JDBC.
 * Chỉ quản lý các thực thể của sales schema (invoices, invoice_lines, invoice_line_batches, payments)
 * cùng native query tra cứu giá và snapshot.
 */
@Repository
class SaleRepositoryAdapter implements SaleRepository {

    private final InvoiceJpaRepository invoiceRepo;
    private final InvoiceLineJpaRepository lineRepo;
    private final InvoiceLineBatchJpaRepository lineBatchRepo;
    private final PaymentJpaRepository paymentRepo;
    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    SaleRepositoryAdapter(InvoiceJpaRepository invoiceRepo,
                          InvoiceLineJpaRepository lineRepo,
                          InvoiceLineBatchJpaRepository lineBatchRepo,
                          PaymentJpaRepository paymentRepo,
                          JdbcTemplate jdbc) {
        this.invoiceRepo = invoiceRepo;
        this.lineRepo = lineRepo;
        this.lineBatchRepo = lineBatchRepo;
        this.paymentRepo = paymentRepo;
        this.jdbc = jdbc;
    }

    // -------------------------------------------------------------------------
    // Price & catalog lookups
    // -------------------------------------------------------------------------

    @Override
    public Optional<Long> findCurrentPrice(UUID orgId, UUID storeId, UUID productId, Instant at) {
        var rows = jdbc.queryForList("""
                SELECT sale_price FROM catalog.product_prices
                WHERE organization_id = ? AND store_id = ? AND product_id = ?
                  AND effective_from <= ?
                  AND (effective_to IS NULL OR effective_to > ?)
                ORDER BY effective_from DESC LIMIT 1
                """, orgId, storeId, productId,
                java.sql.Timestamp.from(at), java.sql.Timestamp.from(at));
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(((Number) rows.get(0).get("sale_price")).longValue());
    }

    @Override
    public Optional<ProductSnapshot> findProductSnapshot(UUID orgId, UUID productId) {
        var rows = jdbc.queryForList("""
                SELECT p.sku, p.name, coalesce(u.precision_scale, 3) as precision_scale
                FROM catalog.products p
                LEFT JOIN catalog.units u ON p.organization_id = u.organization_id AND p.base_unit_id = u.id
                WHERE p.organization_id = ? AND p.id = ? AND p.status = 'ACTIVE'
                """, orgId, productId);
        if (rows.isEmpty()) return Optional.empty();
        var row = rows.get(0);
        int precisionScale = ((Number) row.get("precision_scale")).intValue();
        return Optional.of(new ProductSnapshot(
                productId, (String) row.get("sku"), (String) row.get("name"), precisionScale));
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Override
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepo.save(new InvoiceJpa(invoice)).toDomain();
    }

    @Override
    public InvoiceLine saveInvoiceLine(InvoiceLine line) {
        return lineRepo.save(new InvoiceLineJpa(line)).toDomain();
    }

    @Override
    public InvoiceLineBatch saveInvoiceLineBatch(InvoiceLineBatch lineBatch) {
        InvoiceLineBatch saved = lineBatchRepo.save(new InvoiceLineBatchJpa(lineBatch)).toDomain();
        // Flush JPA to DB so cross-module stock_movements insert can satisfy FK constraint
        entityManager.flush();
        return saved;
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepo.save(new PaymentJpa(payment)).toDomain();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    public Optional<Invoice> findInvoiceById(UUID orgId, UUID id) {
        return invoiceRepo.findByOrganizationIdAndId(orgId, id).map(InvoiceJpa::toDomain);
    }

    @Override
    public List<Invoice> listInvoices(UUID orgId, UUID storeId) {
        return invoiceRepo.findByOrganizationIdAndStoreId(orgId, storeId)
                .stream().map(InvoiceJpa::toDomain).toList();
    }

    @Override
    public List<Invoice> listInvoicesBySeller(UUID orgId, UUID storeId, UUID soldBy) {
        return invoiceRepo.findByOrganizationIdAndStoreIdAndSoldBy(orgId, storeId, soldBy)
                .stream().map(InvoiceJpa::toDomain).toList();
    }

    @Override
    public List<InvoiceLine> findLinesByInvoiceId(UUID orgId, UUID invoiceId) {
        return lineRepo.findByOrganizationIdAndInvoiceId(orgId, invoiceId)
                .stream().map(InvoiceLineJpa::toDomain).toList();
    }

    @Override
    public List<InvoiceLineBatch> findLineBatchesByInvoiceLineId(UUID orgId, UUID invoiceLineId) {
        return lineBatchRepo.findByOrganizationIdAndInvoiceLineId(orgId, invoiceLineId)
                .stream().map(InvoiceLineBatchJpa::toDomain).toList();
    }

    @Override
    public List<Payment> findPaymentsByInvoiceId(UUID orgId, UUID invoiceId) {
        return paymentRepo.findByOrganizationIdAndInvoiceId(orgId, invoiceId)
                .stream().map(PaymentJpa::toDomain).toList();
    }
}
