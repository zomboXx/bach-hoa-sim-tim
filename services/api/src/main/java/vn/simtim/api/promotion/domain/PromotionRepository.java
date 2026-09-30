package vn.simtim.api.promotion.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    List<Promotion> findByOrganization(UUID organizationId);
    Optional<Promotion> findById(UUID organizationId, UUID id);
    boolean existsByCode(UUID organizationId, String code);
    boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId);
    Promotion save(Promotion promotion);
    void deleteById(UUID organizationId, UUID id);

    List<PromotionProduct> findProductsByPromotion(UUID organizationId, UUID promotionId);
    boolean existsProduct(UUID organizationId, UUID promotionId, UUID productId);
    void saveProduct(PromotionProduct pp);
    void deleteProduct(UUID organizationId, UUID promotionId, UUID productId);

    /**
     * Tìm các khuyến mãi đang hiệu lực tại thời điểm {@code at} áp dụng cho sản phẩm và cửa hàng.
     * Bao gồm khuyến mãi toàn sản phẩm (không giới hạn phạm vi sản phẩm).
     */
    List<Promotion> findApplicable(UUID organizationId, UUID storeId, UUID productId, Instant at);
}
