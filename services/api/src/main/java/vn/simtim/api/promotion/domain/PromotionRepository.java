package vn.simtim.api.promotion.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    /** Danh sách khuyến mãi thuộc đúng org + store của session. */
    List<Promotion> findByStore(UUID organizationId, UUID storeId);

    /** Tìm theo ID, phải thuộc đúng org + store. */
    Optional<Promotion> findById(UUID organizationId, UUID storeId, UUID id);

    boolean existsByCode(UUID organizationId, UUID storeId, String code);
    boolean existsByCodeExcluding(UUID organizationId, UUID storeId, String code, UUID excludeId);
    Promotion save(Promotion promotion);
    void deleteById(UUID organizationId, UUID storeId, UUID id);

    /** Danh sách sản phẩm trong phạm vi khuyến mãi; kiểm tra promotion thuộc đúng store. */
    List<PromotionProduct> findProductsByPromotion(UUID organizationId, UUID storeId, UUID promotionId);
    boolean existsProduct(UUID organizationId, UUID promotionId, UUID productId);
    void saveProduct(PromotionProduct pp);
    void deleteProduct(UUID organizationId, UUID promotionId, UUID productId);

    /**
     * Tìm các khuyến mãi đang hiệu lực tại thời điểm {@code at} áp dụng cho sản phẩm và cửa hàng.
     * Bao gồm khuyến mãi toàn sản phẩm (không giới hạn phạm vi sản phẩm).
     */
    List<Promotion> findApplicable(UUID organizationId, UUID storeId, UUID productId, Instant at);
}
