package vn.simtim.api.promotion.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PromotionJpaRepository extends JpaRepository<PromotionJpa, UUID> {

    /** Danh sách theo org + store (store null = toàn tổ chức, đã lọc đúng store). */
    @Query("SELECT p FROM PromotionJpa p WHERE p.organizationId = :orgId AND p.storeId = :storeId")
    List<PromotionJpa> findByOrganizationIdAndStoreId(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId);

    @Query("SELECT p FROM PromotionJpa p WHERE p.organizationId = :orgId AND p.storeId = :storeId AND p.id = :id")
    Optional<PromotionJpa> findByOrganizationIdAndStoreIdAndId(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId, @Param("id") UUID id);

    @Query("SELECT COUNT(p) > 0 FROM PromotionJpa p " +
           "WHERE p.organizationId = :orgId AND p.storeId = :storeId AND lower(p.code) = lower(:code)")
    boolean existsByCodeInsensitive(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId, @Param("code") String code);

    @Query("SELECT COUNT(p) > 0 FROM PromotionJpa p " +
           "WHERE p.organizationId = :orgId AND p.storeId = :storeId " +
           "  AND lower(p.code) = lower(:code) AND p.id <> :excludeId")
    boolean existsByCodeInsensitiveExcluding(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId,
            @Param("code") String code, @Param("excludeId") UUID excludeId);

    /**
     * Khuyến mãi ACTIVE trong khung thời gian, đúng cửa hàng, KHÔNG giới hạn sản phẩm
     * (không có dòng nào trong promotion_products).
     */
    @Query("SELECT p FROM PromotionJpa p " +
           "WHERE p.organizationId = :orgId " +
           "  AND p.status = 'ACTIVE' " +
           "  AND p.startsAt <= :at AND p.endsAt > :at " +
           "  AND (p.storeId IS NULL OR p.storeId = :storeId) " +
           "  AND NOT EXISTS (" +
           "      SELECT pp FROM PromotionProductJpa pp WHERE pp.id.promotionId = p.id)")
    List<PromotionJpa> findActiveUniversal(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId, @Param("at") Instant at);

    /**
     * Khuyến mãi ACTIVE trong khung thời gian, đúng cửa hàng, CÓ sản phẩm cụ thể trong phạm vi.
     */
    @Query("SELECT DISTINCT p FROM PromotionJpa p " +
           "JOIN PromotionProductJpa pp ON pp.id.promotionId = p.id " +
           "WHERE p.organizationId = :orgId " +
           "  AND p.status = 'ACTIVE' " +
           "  AND p.startsAt <= :at AND p.endsAt > :at " +
           "  AND (p.storeId IS NULL OR p.storeId = :storeId) " +
           "  AND pp.id.productId = :productId")
    List<PromotionJpa> findActiveForProduct(
            @Param("orgId") UUID orgId, @Param("storeId") UUID storeId,
            @Param("productId") UUID productId, @Param("at") Instant at);
}
