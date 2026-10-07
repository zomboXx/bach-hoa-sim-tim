package vn.simtim.api.sale.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.simtim.api.sale.domain.InvoiceLine;

@Entity
@Table(schema = "sales", name = "invoice_lines")
class InvoiceLineJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id", nullable = false) UUID storeId;
    @Column(name = "invoice_id", nullable = false) UUID invoiceId;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(name = "sku_snapshot", nullable = false) String skuSnapshot;
    @Column(name = "product_name_snapshot", nullable = false) String productNameSnapshot;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal quantity;
    @Column(name = "unit_price", nullable = false) long unitPrice;
    @Column(name = "discount_amount", nullable = false) long discountAmount;
    @Column(name = "line_total", nullable = false) long lineTotal;
    @Column(name = "applied_promotion_id") UUID appliedPromotionId;
    @Column(name = "applied_promotion_code", length = 60) String appliedPromotionCode;
    @Column(name = "applied_promotion_name", length = 200) String appliedPromotionName;
    @Column(name = "promotion_discount_type", length = 16) String promotionDiscountType;
    @Column(name = "promotion_discount_value", precision = 14, scale = 2) BigDecimal promotionDiscountValue;

    InvoiceLineJpa() {}

    InvoiceLineJpa(InvoiceLine l) {
        this.id = l.id();
        this.organizationId = l.organizationId();
        this.storeId = l.storeId();
        this.invoiceId = l.invoiceId();
        this.productId = l.productId();
        this.skuSnapshot = l.skuSnapshot();
        this.productNameSnapshot = l.productNameSnapshot();
        this.quantity = l.quantity();
        this.unitPrice = l.unitPrice();
        this.discountAmount = l.discountAmount();
        this.lineTotal = l.lineTotal();
        this.appliedPromotionId = l.appliedPromotionId();
        this.appliedPromotionCode = l.appliedPromotionCode();
        this.appliedPromotionName = l.appliedPromotionName();
        this.promotionDiscountType = l.promotionDiscountType();
        this.promotionDiscountValue = l.promotionDiscountValue();
    }

    InvoiceLine toDomain() {
        return new InvoiceLine(id, organizationId, storeId, invoiceId, productId,
                skuSnapshot, productNameSnapshot, quantity, unitPrice, discountAmount,
                lineTotal, appliedPromotionId, appliedPromotionCode, appliedPromotionName,
                promotionDiscountType, promotionDiscountValue);
    }
}
