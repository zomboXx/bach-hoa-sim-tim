package vn.simtim.api.sale.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.FefoAllocation;
import vn.simtim.api.inventory.application.FefoPlan;
import vn.simtim.api.inventory.application.InventoryPort;
import vn.simtim.api.inventory.application.InventorySalePort;
import vn.simtim.api.inventory.application.SaleIssue;
import vn.simtim.api.inventory.application.StockDemand;
import vn.simtim.api.inventory.application.StoreScope;
import vn.simtim.api.promotion.domain.Promotion;
import vn.simtim.api.promotion.domain.PromotionRepository;
import vn.simtim.api.sale.domain.Invoice;
import vn.simtim.api.sale.domain.InvoiceLine;
import vn.simtim.api.sale.domain.InvoiceLineBatch;
import vn.simtim.api.sale.domain.Payment;
import vn.simtim.api.sale.domain.ProductSnapshot;
import vn.simtim.api.sale.domain.SaleConflictException;
import vn.simtim.api.sale.domain.SaleNotFoundException;
import vn.simtim.api.sale.domain.SaleRepository;
import vn.simtim.api.sale.domain.SaleValidationException;

/** Sales use cases. FEFO always selects stock before a promotion is chosen. */
@Service
@Transactional
public class SaleService {

    private final SaleRepository repo;
    private final InventoryPort quoteInventory;
    private final InventorySalePort checkoutInventory;
    private final PromotionRepository promotions;

    public SaleService(
            SaleRepository repo,
            InventoryPort quoteInventory,
            InventorySalePort checkoutInventory,
            PromotionRepository promotions) {
        this.repo = repo;
        this.quoteInventory = quoteInventory;
        this.checkoutInventory = checkoutInventory;
        this.promotions = promotions;
    }

    @Transactional(readOnly = true)
    public QuoteResult quote(UUID orgId, UUID storeId, List<CheckoutItem> items) {
        validateItems(items);
        Instant capturedAt = Instant.now();
        List<QuoteResult.QuoteLineResult> lines = new ArrayList<>();
        for (CheckoutItem item : mergeItems(items)) {
            ProductSnapshot snapshot = resolveSnapshot(orgId, item.productId());
            long unitPrice = resolvePrice(orgId, storeId, item.productId(), capturedAt);
            BigDecimal remaining = item.quantity();
            for (var batch : quoteInventory.findAvailableBatchesFEFO(orgId, storeId, item.productId())) {
                if (remaining.signum() == 0) break;
                BigDecimal quantity = remaining.min(batch.quantityOnHand());
                PromotionChoice promotion = bestPromotion(
                        orgId, storeId, item.productId(), batch.batchId(), capturedAt, unitPrice, quantity);
                long gross = gross(unitPrice, quantity);
                lines.add(new QuoteResult.QuoteLineResult(
                        item.productId(), snapshot.name(), batch.batchId(), quantity, unitPrice,
                        gross, promotion.discountAmount(), gross - promotion.discountAmount(),
                        promotion.id(), promotion.code(), promotion.name()));
                remaining = remaining.subtract(quantity);
            }
            if (remaining.signum() > 0) {
                throw new SaleConflictException("Không đủ tồn kho cho sản phẩm SKU: " + snapshot.sku());
            }
        }
        return quoteResult(storeId, lines);
    }

    public Invoice checkout(
            UUID orgId, UUID storeId, UUID soldByUserId, List<CheckoutItem> items, long cashAmount) {
        validateItems(items);
        StoreScope scope = new StoreScope(orgId, storeId);
        FefoPlan plan = checkoutInventory.planForCheckout(scope, toDemands(items));
        Map<UUID, ProductSnapshot> snapshots = new LinkedHashMap<>();
        Map<UUID, Long> prices = new LinkedHashMap<>();
        for (FefoAllocation allocation : plan.allocations()) {
            snapshots.computeIfAbsent(allocation.productId(), id -> resolveSnapshot(orgId, id));
            prices.computeIfAbsent(allocation.productId(), id -> resolvePrice(orgId, storeId, id, plan.capturedAt()));
        }

        List<LineData> lineData = new ArrayList<>();
        long subtotal = 0;
        long discountTotal = 0;
        for (FefoAllocation allocation : plan.allocations()) {
            long unitPrice = prices.get(allocation.productId());
            long gross = gross(unitPrice, allocation.quantity());
            PromotionChoice promotion = bestPromotion(
                    orgId, storeId, allocation.productId(), allocation.batchId(), plan.capturedAt(),
                    unitPrice, allocation.quantity());
            subtotal = Math.addExact(subtotal, gross);
            discountTotal = Math.addExact(discountTotal, promotion.discountAmount());
            lineData.add(new LineData(allocation, snapshots.get(allocation.productId()), unitPrice, gross, promotion));
        }
        long grandTotal = Math.subtractExact(subtotal, discountTotal);
        if (cashAmount < grandTotal) {
            throw new SaleValidationException(
                    "Số tiền nhận (" + cashAmount + ") nhỏ hơn tổng hoá đơn (" + grandTotal + ")");
        }

        Instant now = plan.capturedAt();
        Invoice invoice = repo.saveInvoice(new Invoice(
                UUID.randomUUID(), orgId, storeId,
                "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), "COMPLETED",
                soldByUserId, now, subtotal, discountTotal, grandTotal, grandTotal,
                cashAmount - grandTotal, List.of(), List.of(), 0L));

        List<SaleIssue> issues = new ArrayList<>();
        for (LineData data : lineData) {
            InvoiceLine saved = repo.saveInvoiceLine(new InvoiceLine(
                    UUID.randomUUID(), orgId, storeId, invoice.id(), data.allocation().productId(),
                    data.snapshot().sku(), data.snapshot().name(), data.allocation().quantity(),
                    data.unitPrice(), data.promotion().discountAmount(),
                    data.gross() - data.promotion().discountAmount(), data.promotion().id(),
                    data.promotion().code(), data.promotion().name(),
                    data.promotion().discountType(), data.promotion().discountValue()));
            repo.saveInvoiceLineBatch(new InvoiceLineBatch(
                    orgId, storeId, saved.id(), data.allocation().productId(),
                    data.allocation().batchId(), data.allocation().quantity()));
            issues.add(new SaleIssue(UUID.randomUUID(), saved.id(), data.allocation().productId(),
                    data.allocation().batchId(), data.allocation().quantity(), soldByUserId));
        }
        repo.savePayment(new Payment(UUID.randomUUID(), orgId, storeId, invoice.id(),
                "CASH", "COMPLETED", grandTotal, now));
        checkoutInventory.postSale(plan, issues);
        return getInvoice(orgId, storeId, invoice.id());
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(UUID orgId, UUID storeId, UUID id) {
        var invoice = repo.findInvoiceById(orgId, id)
                .orElseThrow(() -> new SaleNotFoundException("Hóa đơn không tồn tại: " + id));
        if (storeId != null && !invoice.storeId().equals(storeId)) {
            throw new SaleNotFoundException("Hóa đơn không tồn tại: " + id);
        }
        return withDetails(orgId, invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(UUID orgId, UUID id) {
        return getInvoice(orgId, null, id);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(UUID orgId, UUID storeId) {
        return repo.listInvoices(orgId, storeId).stream().map(invoice -> withDetails(orgId, invoice)).toList();
    }

    private Invoice withDetails(UUID orgId, Invoice invoice) {
        return new Invoice(invoice.id(), invoice.organizationId(), invoice.storeId(), invoice.invoiceNo(),
                invoice.status(), invoice.soldBy(), invoice.soldAt(), invoice.subtotal(),
                invoice.discountTotal(), invoice.grandTotal(), invoice.paidTotal(), invoice.changeAmount(),
                repo.findLinesByInvoiceId(orgId, invoice.id()),
                repo.findPaymentsByInvoiceId(orgId, invoice.id()), invoice.version());
    }

    private QuoteResult quoteResult(UUID storeId, List<QuoteResult.QuoteLineResult> lines) {
        long subtotal = lines.stream().mapToLong(QuoteResult.QuoteLineResult::grossAmount).sum();
        long discountTotal = lines.stream().mapToLong(QuoteResult.QuoteLineResult::discountAmount).sum();
        return new QuoteResult(storeId, lines, subtotal, discountTotal, subtotal - discountTotal);
    }

    private PromotionChoice bestPromotion(
            UUID orgId, UUID storeId, UUID productId, UUID batchId, Instant at,
            long unitPrice, BigDecimal quantity) {
        long gross = gross(unitPrice, quantity);
        return promotions.findApplicable(orgId, storeId, productId, batchId, at).stream()
                .map(promotion -> promotionChoice(promotion, unitPrice, quantity, gross))
                .max(Comparator.comparingLong(PromotionChoice::discountAmount)
                        .thenComparing(PromotionChoice::id, Comparator.reverseOrder()))
                .orElse(PromotionChoice.none());
    }

    private PromotionChoice promotionChoice(
            Promotion promotion, long unitPrice, BigDecimal quantity, long gross) {
        BigDecimal perUnit = "PERCENT".equals(promotion.discountType())
                ? BigDecimal.valueOf(unitPrice).multiply(promotion.discountValue())
                        .divide(BigDecimal.valueOf(100))
                : promotion.discountValue();
        long discount = quantity.multiply(perUnit).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new PromotionChoice(promotion.id(), promotion.code(), promotion.name(),
                promotion.discountType(), promotion.discountValue(), Math.min(gross, discount));
    }

    private List<StockDemand> toDemands(List<CheckoutItem> items) {
        return mergeItems(items).stream().map(item -> new StockDemand(item.productId(), item.quantity())).toList();
    }

    private List<CheckoutItem> mergeItems(List<CheckoutItem> items) {
        Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
        for (CheckoutItem item : items) quantities.merge(item.productId(), item.quantity(), BigDecimal::add);
        return quantities.entrySet().stream().map(entry -> new CheckoutItem(entry.getKey(), entry.getValue())).toList();
    }

    private void validateItems(List<CheckoutItem> items) {
        if (items == null || items.isEmpty()) throw new SaleValidationException("Đơn hàng không có sản phẩm");
        for (CheckoutItem item : items) {
            if (item.quantity() == null || item.quantity().signum() <= 0 || item.quantity().scale() > 3) {
                throw new SaleValidationException("Số lượng không hợp lệ cho sản phẩm: " + item.productId());
            }
        }
    }

    private long resolvePrice(UUID orgId, UUID storeId, UUID productId, Instant at) {
        return repo.findCurrentPrice(orgId, storeId, productId, at).orElseThrow(
                () -> new SaleValidationException("Không tìm thấy giá bán cho sản phẩm: " + productId));
    }

    private ProductSnapshot resolveSnapshot(UUID orgId, UUID productId) {
        return repo.findProductSnapshot(orgId, productId).orElseThrow(
                () -> new SaleValidationException("Sản phẩm không tồn tại: " + productId));
    }

    private long gross(long unitPrice, BigDecimal quantity) {
        return quantity.multiply(BigDecimal.valueOf(unitPrice)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private record LineData(
            FefoAllocation allocation, ProductSnapshot snapshot, long unitPrice, long gross,
            PromotionChoice promotion) {}

    private record PromotionChoice(
            UUID id, String code, String name, String discountType, BigDecimal discountValue,
            long discountAmount) {
        static PromotionChoice none() {
            return new PromotionChoice(null, null, null, null, null, 0L);
        }
    }
}
