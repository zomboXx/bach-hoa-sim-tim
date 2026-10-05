package vn.simtim.api.promotion.infrastructure;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.simtim.api.promotion.domain.Promotion;
import vn.simtim.api.promotion.domain.PromotionBatch;
import vn.simtim.api.promotion.domain.PromotionProduct;
import vn.simtim.api.promotion.domain.PromotionRepository;

@Repository
class PromotionRepositoryAdapter implements PromotionRepository {

    private final PromotionJpaRepository jpa;
    private final PromotionProductJpaRepository productJpa;
    private final PromotionBatchJpaRepository batchJpa;

    PromotionRepositoryAdapter(
            PromotionJpaRepository jpa,
            PromotionProductJpaRepository productJpa,
            PromotionBatchJpaRepository batchJpa) {
        this.jpa = jpa;
        this.productJpa = productJpa;
        this.batchJpa = batchJpa;
    }

    @Override
    public List<Promotion> findByStore(UUID organizationId, UUID storeId) {
        return jpa.findByOrganizationIdAndStoreId(organizationId, storeId)
                .stream().map(PromotionJpa::toDomain).toList();
    }

    @Override
    public Optional<Promotion> findById(UUID organizationId, UUID storeId, UUID id) {
        return jpa.findByOrganizationIdAndStoreIdAndId(organizationId, storeId, id)
                .map(PromotionJpa::toDomain);
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
    public void deleteById(UUID organizationId, UUID storeId, UUID id) {
        jpa.findByOrganizationIdAndStoreIdAndId(organizationId, storeId, id).ifPresent(e -> {
            productJpa.deleteAllByPromotionId(id);
            batchJpa.deleteAllByPromotionId(id);
            jpa.deleteById(e.id);
        });
    }

    @Override
    public List<PromotionProduct> findProductsByPromotion(UUID organizationId, UUID storeId, UUID promotionId) {
        if (jpa.findByOrganizationIdAndStoreIdAndId(organizationId, storeId, promotionId).isEmpty()) {
            return List.of();
        }
        return productJpa.findByPromotionId(promotionId).stream().map(PromotionProductJpa::toDomain).toList();
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

    @Override
    public List<PromotionBatch> findBatchesByPromotion(UUID organizationId, UUID storeId, UUID promotionId) {
        if (jpa.findByOrganizationIdAndStoreIdAndId(organizationId, storeId, promotionId).isEmpty()) {
            return List.of();
        }
        return batchJpa.findByPromotionId(promotionId).stream().map(PromotionBatchJpa::toDomain).toList();
    }

    @Override
    public boolean existsBatch(UUID organizationId, UUID promotionId, UUID productBatchId) {
        return batchJpa.existsByPromotionIdAndProductBatchId(promotionId, productBatchId);
    }

    @Override
    public void saveBatch(PromotionBatch batch) {
        batchJpa.save(new PromotionBatchJpa(batch));
    }

    @Override
    public void deleteBatch(UUID organizationId, UUID promotionId, UUID productBatchId) {
        batchJpa.deleteByPromotionIdAndProductBatchId(promotionId, productBatchId);
    }

    @Override
    public boolean isReferencedByInvoiceLine(UUID organizationId, UUID promotionId) {
        return jpa.isReferencedByInvoiceLine(organizationId, promotionId);
    }

    @Override
    public List<Promotion> findApplicable(
            UUID organizationId, UUID storeId, UUID productId, UUID productBatchId, Instant at) {
        var universal = jpa.findActiveUniversal(organizationId, storeId, at);
        var specific = jpa.findActiveForProduct(organizationId, storeId, productId, at);
        var batchSpecific = jpa.findActiveForBatch(organizationId, storeId, productBatchId, at);
        var merged = new LinkedHashMap<UUID, PromotionJpa>();
        for (var p : universal) merged.put(p.id, p);
        for (var p : specific) merged.put(p.id, p);
        for (var p : batchSpecific) merged.put(p.id, p);
        return merged.values().stream().map(PromotionJpa::toDomain).toList();
    }
}
