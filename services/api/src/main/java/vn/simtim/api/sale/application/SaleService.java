package vn.simtim.api.sale.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.BatchStock;
import vn.simtim.api.inventory.application.InventoryPort;
import vn.simtim.api.sale.domain.*;

/**
 * Use-case service cho SAL-01: Quote, Checkout CASH, xem hóa đơn.
 * Tương tác với inventory thông qua public InventoryPort trong cùng transactional context.
 */
@Service
@Transactional
public class SaleService {

    private final SaleRepository repo;
    private final InventoryPort inventoryPort;

    public SaleService(SaleRepository repo, InventoryPort inventoryPort) {
        this.repo = repo;
        this.inventoryPort = inventoryPort;
    }

    // -------------------------------------------------------------------------
    // Quote — read-only, không ghi DB
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public QuoteResult quote(UUID orgId, UUID storeId, List<CheckoutItem> items) {
        validateItems(items);
        var lines = new ArrayList<QuoteResult.QuoteLineResult>();
        long subtotal = 0;
        for (var item : items) {
            long unitPrice = resolvePrice(orgId, storeId, item.productId());
            var snap = resolveSnapshot(orgId, item.productId());
            long lineTotal = computeLineTotal(unitPrice, item.quantity());
            subtotal += lineTotal;
            lines.add(new QuoteResult.QuoteLineResult(
                    item.productId(), snap.name(), item.quantity(), unitPrice, lineTotal));
        }
        return new QuoteResult(storeId, lines, subtotal, subtotal);
    }

    // -------------------------------------------------------------------------
    // Checkout CASH — atomic: invoice + batch allocation + stock deduction + payment
    // -------------------------------------------------------------------------

    public Invoice checkout(UUID orgId, UUID storeId, UUID soldByUserId,
                            List<CheckoutItem> items, long cashAmount) {
        validateItems(items);

        // 0. Quick price pass to validate cashAmount upfront (avoids locking stock for an invalid payment)
        long estimatedSubtotal = 0;
        for (var item : items) {
            long unitPrice = resolvePrice(orgId, storeId, item.productId());
            estimatedSubtotal += computeLineTotal(unitPrice, item.quantity());
        }
        if (cashAmount < estimatedSubtotal) {
            throw new SaleValidationException(
                    "Số tiền nhận (" + cashAmount + ") nhỏ hơn tổng hoá đơn (" + estimatedSubtotal + ")");
        }

        // 1. Resolve price + snapshot; allocate batches FEFO through InventoryPort
        record LineData(CheckoutItem item, ProductSnapshot snap, long unitPrice,
                        long lineTotal, List<InvoiceLineBatch> batchAllocs) {}
        var lineDataList = new ArrayList<LineData>();
        long subtotal = 0;

        for (var item : items) {
            long unitPrice = resolvePrice(orgId, storeId, item.productId());
            var snap = resolveSnapshot(orgId, item.productId());
            long lineTotal = computeLineTotal(unitPrice, item.quantity());
            subtotal += lineTotal;

            // Fetch available FEFO batches from InventoryPort
            var batches = inventoryPort.findAvailableBatchesFEFO(orgId, storeId, item.productId());
            var allocs = allocateFEFO(orgId, storeId, item.productId(), item.quantity(), batches, snap.sku());

            lineDataList.add(new LineData(item, snap, unitPrice, lineTotal, allocs));
        }

        // 2. Create invoice
        Instant now = Instant.now();
        String invoiceNo = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long discountTotal = 0L;
        long grandTotal = subtotal - discountTotal;
        long changeAmount = cashAmount - grandTotal;
        long paidTotal = grandTotal;

        Invoice invoice = repo.saveInvoice(new Invoice(
                UUID.randomUUID(), orgId, storeId, invoiceNo, "COMPLETED",
                soldByUserId, now, subtotal, discountTotal, grandTotal, paidTotal, changeAmount,
                List.of(), List.of(), 0L));

        // 3. Save lines, batch allocations, deduct stock, record movements via InventoryPort
        for (var ld : lineDataList) {
            InvoiceLine savedLine = repo.saveInvoiceLine(new InvoiceLine(
                UUID.randomUUID(), orgId, storeId, invoice.id(), ld.item().productId(),
                ld.snap().sku(), ld.snap().name(), ld.item().quantity(),
                ld.unitPrice(), 0L, ld.lineTotal(), null));

            for (var alloc : ld.batchAllocs()) {
                InvoiceLineBatch withLineId = new InvoiceLineBatch(
                        alloc.organizationId(), alloc.storeId(), savedLine.id(),
                        alloc.productId(), alloc.productBatchId(), alloc.quantity());
                repo.saveInvoiceLineBatch(withLineId);
                inventoryPort.deductBalance(orgId, storeId, alloc.productBatchId(), alloc.quantity());
                inventoryPort.recordSaleMovement(
                        UUID.randomUUID(), orgId, storeId, alloc.productBatchId(),
                        alloc.quantity().negate(), savedLine.id(), soldByUserId, now);
            }
        }

        // 4. Create payment — recorded amount is grandTotal, changeAmount returned to customer
        repo.savePayment(new Payment(UUID.randomUUID(), orgId, storeId, invoice.id(),
                "CASH", "COMPLETED", grandTotal, now));

        // 5. Return full invoice with lines and payments loaded
        return getInvoice(orgId, storeId, invoice.id());
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Invoice getInvoice(UUID orgId, UUID storeId, UUID id) {
        var inv = repo.findInvoiceById(orgId, id)
                .orElseThrow(() -> new SaleNotFoundException("Hóa đơn không tồn tại: " + id));
        if (storeId != null && !inv.storeId().equals(storeId)) {
            throw new SaleNotFoundException("Hóa đơn không tồn tại: " + id);
        }
        var lines = loadLines(orgId, id);
        var payments = repo.findPaymentsByInvoiceId(orgId, id);
        return new Invoice(inv.id(), inv.organizationId(), inv.storeId(), inv.invoiceNo(),
                inv.status(), inv.soldBy(), inv.soldAt(), inv.subtotal(), inv.discountTotal(),
                inv.grandTotal(), inv.paidTotal(), inv.changeAmount(), lines, payments, inv.version());
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(UUID orgId, UUID id) {
        return getInvoice(orgId, null, id);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(UUID orgId, UUID storeId) {
        return repo.listInvoices(orgId, storeId).stream()
                .map(inv -> {
                    var lines = loadLines(orgId, inv.id());
                    var payments = repo.findPaymentsByInvoiceId(orgId, inv.id());
                    return new Invoice(inv.id(), inv.organizationId(), inv.storeId(), inv.invoiceNo(),
                            inv.status(), inv.soldBy(), inv.soldAt(), inv.subtotal(), inv.discountTotal(),
                            inv.grandTotal(), inv.paidTotal(), inv.changeAmount(), lines, payments, inv.version());
                }).toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateItems(List<CheckoutItem> items) {
        if (items == null || items.isEmpty()) {
            throw new SaleValidationException("Đơn hàng không có sản phẩm");
        }
        for (var item : items) {
            if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SaleValidationException(
                        "Số lượng không hợp lệ cho sản phẩm: " + item.productId());
            }
        }
    }

    private long resolvePrice(UUID orgId, UUID storeId, UUID productId) {
        return repo.findCurrentPrice(orgId, storeId, productId, Instant.now())
                .orElseThrow(() -> new SaleValidationException(
                        "Không tìm thấy giá bán cho sản phẩm: " + productId));
    }

    private ProductSnapshot resolveSnapshot(UUID orgId, UUID productId) {
        return repo.findProductSnapshot(orgId, productId)
                .orElseThrow(() -> new SaleValidationException(
                        "Sản phẩm không tồn tại: " + productId));
    }

    /** Tính lineTotal = unitPrice × quantity, làm tròn xuống VND. */
    private long computeLineTotal(long unitPrice, BigDecimal quantity) {
        return quantity.multiply(BigDecimal.valueOf(unitPrice))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * Phân bổ số lượng từ danh sách lô FEFO.
     * Mỗi lô được lấy tối đa quantity_on_hand.
     */
    private List<InvoiceLineBatch> allocateFEFO(UUID orgId, UUID storeId, UUID productId,
                                                 BigDecimal needed,
                                                 List<BatchStock> batches, String sku) {
        var allocs = new ArrayList<InvoiceLineBatch>();
        BigDecimal remaining = needed;
        for (var b : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = remaining.min(b.quantityOnHand());
            allocs.add(new InvoiceLineBatch(orgId, storeId, null /* set later */, productId, b.batchId(), take));
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new SaleConflictException("Không đủ tồn kho cho sản phẩm SKU: " + sku
                    + " (thiếu " + remaining + ")");
        }
        return allocs;
    }

    private List<InvoiceLine> loadLines(UUID orgId, UUID invoiceId) {
        return repo.findLinesByInvoiceId(orgId, invoiceId).stream()
                .map(line -> {
                    var lineBatches = repo.findLineBatchesByInvoiceLineId(orgId, line.id());
                    return line;
                }).toList();
    }
}
