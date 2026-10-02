package vn.simtim.api.promotion.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.promotion.domain.Promotion;
import vn.simtim.api.promotion.domain.PromotionProduct;
import vn.simtim.api.promotion.domain.PromotionRepository;

@Repository
class PromotionRepositoryAdapter implements PromotionRepository {

    private final PromotionJpaRepository jpa;
    private final PromotionProductJpaRepository productJpa;

    PromotionRepositoryAdapter(PromotionJpaRepository jpa, PromotionProductJpaRepository productJpa) {
        this.jpa = jpa;
        this.productJpa = productJpa;
    }

    @Override
    public List<Promotion> findByOrganization(UUID organizationId) {
        return jpa.findByOrganizationId(organizationId).stream().map(PromotionJpa::toDomain).toList();
    }

    @Override
    public Optional<Promotion> findById(UUID organizationId, UUID id) {
        return jpa.findByOrganizationIdAndId(organizationId, id).map(PromotionJpa::toDomain);
    }

    @Override
    public boolean existsByCode(UUID organizationId, String code) {
        return jpa.existsByCodeInsensitive(organizationId, code);
    }

    @Override
    public boolean existsByCodeExcluding(UUID organizationId, String code, UUID excludeId) {
        return jpa.existsByCodeInsensitiveExcluding(organizationId, code, excludeId);
    }

    @Override
    public Promotion save(Promotion promotion) {
        return jpa.save(new PromotionJpa(promotion)).toDomain();
    }

    @Override
    public void deleteById(UUID organizationId, UUID id) {
        jpa.findByOrganizationIdAndId(organizationId, id).ifPresent(e -> {
            productJpa.deleteAllByPromotionId(id);
            jpa.deleteById(e.id);
        });
    }

    @Override
    public List<PromotionProduct> findProductsByPromotion(UUID organizationId, UUID promotionId) {
        return productJpa.findByPromotionId(promotionId).stream()
                .map(PromotionProductJpa::toDomain).toList();
    }

    @Override
    public boolean existsProduct(UUID organizationId, UUID promotionId, UUID productId) {
        return productJpa.existsByPromotionIdAndProductId(promotionId, productId);
    }

    @Override
    public void saveProduct(PromotionProduct pp) {
        productJpa.save(new PromotionProductJpa(pp));
    }

    @Override
    public void deleteProduct(UUID organizationId, UUID promotionId, UUID productId) {
        productJpa.deleteByPromotionIdAndProductId(promotionId, productId);
    }

    /**
     * Ghép hai tập kết quả:
     * 1. Khuyến mãi không giới hạn sản phẩm (universal)
     * 2. Khuyến mãi có sản phẩm cụ thể trong phạm vi
     * Loại bỏ trùng theo id.
     */
    @Override
    public List<Promotion> findApplicable(UUID organizationId, UUID storeId, UUID productId, Instant at) {
        var universal = jpa.findActiveUniversal(organizationId, storeId, at);
        var specific = jpa.findActiveForProduct(organizationId, storeId, productId, at);
        var merged = new LinkedHashMap<UUID, PromotionJpa>();
        for (var p : universal) merged.put(p.id, p);
        for (var p : specific) merged.put(p.id, p);
        return merged.values().stream().map(PromotionJpa::toDomain).toList();
    }
}
