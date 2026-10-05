package vn.simtim.api.promotion.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.ProductRepository;
import vn.simtim.api.promotion.domain.*;

/**
 * Use-case service cho PRO-01B: CRUD khuyến mãi, quản lý phạm vi sản phẩm và tra
 * cứu khuyến mãi hiệu lực tại điểm bán.
 *
 * Mọi thao tác đọc/ghi đều scoped theo (organizationId, storeId) từ SessionPrincipal —
 * không truy cập khuyến mãi của cửa hàng khác trong cùng tổ chức.
 */
@Service
@Transactional
public class PromotionService {

    private final PromotionRepository repo;
    private final ProductRepository productRepo;

    public PromotionService(PromotionRepository repo, ProductRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    // ===== Queries =====

    @Transactional(readOnly = true)
    public List<Promotion> listByStore(UUID organizationId, UUID storeId) {
        return repo.findByStore(organizationId, storeId);
    }

    @Transactional(readOnly = true)
    public Promotion getById(UUID organizationId, UUID storeId, UUID id) {
        return repo.findById(organizationId, storeId, id)
                .orElseThrow(() -> new PromotionNotFoundException("Khuyến mãi không tồn tại: " + id));
    }

    @Transactional(readOnly = true)
    public List<PromotionProduct> listProducts(UUID organizationId, UUID storeId, UUID promotionId) {
        getById(organizationId, storeId, promotionId); // ensure exists and belongs to store
        return repo.findProductsByPromotion(organizationId, storeId, promotionId);
    }

    @Transactional(readOnly = true)
    public List<PromotionBatch> listBatches(UUID organizationId, UUID storeId, UUID promotionId) {
        getById(organizationId, storeId, promotionId);
        return repo.findBatchesByPromotion(organizationId, storeId, promotionId);
    }

    /**
     * Trả về danh sách khuyến mãi hiệu lực tại {@code at} cho sản phẩm + cửa hàng.
     * Bao gồm cả khuyến mãi không giới hạn phạm vi sản phẩm (toàn sản phẩm).
     * at null → dùng Instant.now().
     */
    @Transactional(readOnly = true)
    public List<Promotion> findApplicable(
            UUID organizationId, UUID storeId, UUID productId, UUID productBatchId, Instant at) {
        Instant effectiveAt = at != null ? at : Instant.now();
        return repo.findApplicable(organizationId, storeId, productId, productBatchId, effectiveAt);
    }

    // ===== Commands =====

    public Promotion create(UUID organizationId, UUID storeId, String code, String name,
                            String discountType, BigDecimal discountValue,
                            Instant startsAt, Instant endsAt, String status) {
        validateTimeWindow(startsAt, endsAt);
        validateDiscountValue(discountType, discountValue);
        if (repo.existsByCode(organizationId, code)) {
            throw new PromotionConflictException("Mã khuyến mãi đã tồn tại: " + code);
        }
        var promotion = new Promotion(UUID.randomUUID(), organizationId, storeId, code, name,
                discountType, discountValue, startsAt, endsAt,
                status != null ? status : "DRAFT");
        return repo.save(promotion);
    }

    public Promotion update(UUID organizationId, UUID storeId, UUID id, String code, String name,
                            String discountType, BigDecimal discountValue,
                            Instant startsAt, Instant endsAt, String status) {
        var existing = getById(organizationId, storeId, id);
        validateTimeWindow(startsAt, endsAt);
        validateDiscountValue(discountType, discountValue);
        if (repo.existsByCodeExcluding(organizationId, code, id)) {
            throw new PromotionConflictException("Mã khuyến mãi đã tồn tại: " + code);
        }
        var updated = new Promotion(id, organizationId, storeId, code, name,
                discountType, discountValue, startsAt, endsAt,
                status != null ? status : existing.status());
        return repo.save(updated);
    }

    public void delete(UUID organizationId, UUID storeId, UUID id) {
        getById(organizationId, storeId, id); // ensure exists and belongs to store
        if (repo.isReferencedByInvoiceLine(organizationId, id)) {
            throw new PromotionConflictException("Không thể xóa khuyến mãi đã xuất hiện trên hóa đơn");
        }
        repo.deleteById(organizationId, storeId, id);
    }

    public void addProduct(UUID organizationId, UUID storeId, UUID promotionId, UUID productId) {
        getById(organizationId, storeId, promotionId);
        if (!repo.findBatchesByPromotion(organizationId, storeId, promotionId).isEmpty()) {
            throw new PromotionValidationException("Khuyến mãi target PRODUCT không thể trộn với target BATCH");
        }
        productRepo.findById(organizationId, productId)
                .orElseThrow(() -> new PromotionNotFoundException("Sản phẩm không tồn tại: " + productId));
        if (repo.existsProduct(organizationId, promotionId, productId)) {
            throw new PromotionConflictException("Sản phẩm đã có trong phạm vi khuyến mãi");
        }
        repo.saveProduct(new PromotionProduct(organizationId, promotionId, productId));
    }

    public void removeProduct(UUID organizationId, UUID storeId, UUID promotionId, UUID productId) {
        getById(organizationId, storeId, promotionId);
        if (!repo.existsProduct(organizationId, promotionId, productId)) {
            throw new PromotionNotFoundException("Sản phẩm không có trong phạm vi khuyến mãi");
        }
        repo.deleteProduct(organizationId, promotionId, productId);
    }

    public void addBatch(UUID organizationId, UUID storeId, UUID promotionId, UUID productBatchId) {
        getById(organizationId, storeId, promotionId);
        if (!repo.findProductsByPromotion(organizationId, storeId, promotionId).isEmpty()) {
            throw new PromotionValidationException("Khuyến mãi target BATCH không thể trộn với target PRODUCT");
        }
        if (repo.existsBatch(organizationId, promotionId, productBatchId)) {
            throw new PromotionConflictException("Lô đã có trong phạm vi khuyến mãi");
        }
        repo.saveBatch(new PromotionBatch(organizationId, promotionId, productBatchId));
    }

    public void removeBatch(UUID organizationId, UUID storeId, UUID promotionId, UUID productBatchId) {
        getById(organizationId, storeId, promotionId);
        if (!repo.existsBatch(organizationId, promotionId, productBatchId)) {
            throw new PromotionNotFoundException("Lô không có trong phạm vi khuyến mãi");
        }
        repo.deleteBatch(organizationId, promotionId, productBatchId);
    }

    // ===== Private validation =====

    private void validateTimeWindow(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new PromotionValidationException("ends_at phải sau starts_at");
        }
    }

    private void validateDiscountValue(String discountType, BigDecimal discountValue) {
        if ("PERCENT".equals(discountType)) {
            // numeric(14,2) chỉ lưu tối đa 2 chữ số thập phân; scale > 2 gây làm tròn
            // ngầm hoặc thành 0 với giá trị rất nhỏ → từ chối sớm.
            if (discountValue.stripTrailingZeros().scale() > 2) {
                throw new PromotionValidationException(
                        "Giá trị giảm theo phần trăm tối đa 2 chữ số thập phân");
            }
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new PromotionValidationException(
                        "Giá trị giảm theo phần trăm không được vượt quá 100");
            }
        }
        if ("AMOUNT".equals(discountType) && discountValue.stripTrailingZeros().scale() > 0) {
            throw new PromotionValidationException("Giá trị giảm theo số tiền phải là số nguyên VND");
        }
    }
}
