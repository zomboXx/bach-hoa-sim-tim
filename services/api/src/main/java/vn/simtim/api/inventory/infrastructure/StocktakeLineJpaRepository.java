package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StocktakeLineJpaRepository extends JpaRepository<StocktakeLineJpa, UUID> {

    List<StocktakeLineJpa> findByStocktakeId(UUID stocktakeId);

    Optional<StocktakeLineJpa> findByStocktakeIdAndBatchId(UUID stocktakeId, UUID batchId);

    @Query("SELECT l FROM StocktakeLineJpa l WHERE l.organizationId = :org "
            + "AND l.storeId = :store AND l.clientOperationId = :coid")
    Optional<StocktakeLineJpa> findByClientOperationId(
            @Param("org") UUID organizationId,
            @Param("store") UUID storeId,
            @Param("coid") UUID clientOperationId);
}
