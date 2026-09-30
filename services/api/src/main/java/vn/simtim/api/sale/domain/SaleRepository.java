package vn.simtim.api.sale.domain;

import java.math.BigDecimal;
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

    /**
     * Danh sách lô còn hàng, đặt khóa PESSIMISTIC_WRITE trên inventory_balances.
     * Trả về theo thứ tự FEFO: expiry_date ASC NULLS LAST, received_date ASC.
     * Chỉ gọi trong transaction đang mở.
     */
    List<BatchStock> findAndLockAvailableBatches(UUID orgId, UUID storeId, UUID productId);

    // --- Write operations (tất cả trong cùng transaction của SaleService) ---

    Invoice saveInvoice(Invoice invoice);

    InvoiceLine saveInvoiceLine(InvoiceLine line);

    InvoiceLineBatch saveInvoiceLineBatch(InvoiceLineBatch lineBatch);

    Payment savePayment(Payment payment);

    /**
     * Trừ {@code quantity} khỏi inventory_balance của lô.
     * Ném SaleConflictException nếu tồn thực tế không đủ.
     */
    void deductBalance(UUID orgId, UUID storeId, UUID batchId, BigDecimal quantity);

    /**
     * Ghi biến động tồn loại SALE cho một (invoice_line, batch) đã lưu.
     * Phải gọi sau saveInvoiceLineBatch để FK thoả mãn.
     */
    void saveStockMovement(UUID id, UUID orgId, UUID storeId, UUID batchId,
                           BigDecimal quantityDelta, UUID invoiceLineId,
                           UUID actorUserId, Instant occurredAt);

    // --- Read operations ---

    Optional<Invoice> findInvoiceById(UUID orgId, UUID id);

    List<Invoice> listInvoices(UUID orgId, UUID storeId);

    List<InvoiceLine> findLinesByInvoiceId(UUID orgId, UUID invoiceId);

    List<InvoiceLineBatch> findLineBatchesByInvoiceLineId(UUID orgId, UUID invoiceLineId);

    List<Payment> findPaymentsByInvoiceId(UUID orgId, UUID invoiceId);
}
