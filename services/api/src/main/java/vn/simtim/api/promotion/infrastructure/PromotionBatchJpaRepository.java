package vn.simtim.api.promotion.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PromotionBatchJpaRepository extends JpaRepository<PromotionBatchJpa, PromotionBatchId> {

    @Query("SELECT pb FROM PromotionBatchJpa pb WHERE pb.id.promotionId = :promotionId")
    List<PromotionBatchJpa> findByPromotionId(@Param("promotionId") UUID promotionId);

    @Query("SELECT COUNT(pb) > 0 FROM PromotionBatchJpa pb "
            + "WHERE pb.id.promotionId = :promotionId AND pb.id.productBatchId = :batchId")
    boolean existsByPromotionIdAndProductBatchId(
            @Param("promotionId") UUID promotionId, @Param("batchId") UUID batchId);

    @Modifying
    @Query("DELETE FROM PromotionBatchJpa pb "
            + "WHERE pb.id.promotionId = :promotionId AND pb.id.productBatchId = :batchId")
    void deleteByPromotionIdAndProductBatchId(
            @Param("promotionId") UUID promotionId, @Param("batchId") UUID batchId);

    @Modifying
    @Query("DELETE FROM PromotionBatchJpa pb WHERE pb.id.promotionId = :promotionId")
    void deleteAllByPromotionId(@Param("promotionId") UUID promotionId);
}
