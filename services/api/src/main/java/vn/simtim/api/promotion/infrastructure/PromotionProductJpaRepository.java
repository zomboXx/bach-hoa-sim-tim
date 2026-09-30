package vn.simtim.api.promotion.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PromotionProductJpaRepository extends JpaRepository<PromotionProductJpa, PromotionProductId> {

    @Query("SELECT pp FROM PromotionProductJpa pp WHERE pp.id.promotionId = :promotionId")
    List<PromotionProductJpa> findByPromotionId(@Param("promotionId") UUID promotionId);

    @Query("SELECT COUNT(pp) > 0 FROM PromotionProductJpa pp " +
           "WHERE pp.id.promotionId = :promotionId AND pp.id.productId = :productId")
    boolean existsByPromotionIdAndProductId(
            @Param("promotionId") UUID promotionId, @Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM PromotionProductJpa pp " +
           "WHERE pp.id.promotionId = :promotionId AND pp.id.productId = :productId")
    void deleteByPromotionIdAndProductId(
            @Param("promotionId") UUID promotionId, @Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM PromotionProductJpa pp WHERE pp.id.promotionId = :promotionId")
    void deleteAllByPromotionId(@Param("promotionId") UUID promotionId);
}
