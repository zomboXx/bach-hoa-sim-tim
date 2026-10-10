package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StocktakeJpaRepository extends JpaRepository<StocktakeJpa, UUID> {

    @Query("SELECT s FROM StocktakeJpa s WHERE s.organizationId = :org "
            + "AND s.storeId = :store AND s.actorId = :actor AND s.status = 'OPEN'")
    Optional<StocktakeJpa> findOpen(
            @Param("org") UUID organizationId,
            @Param("store") UUID storeId,
            @Param("actor") UUID actorId);

    @Query("SELECT s FROM StocktakeJpa s WHERE s.organizationId = :org "
            + "AND s.storeId = :store AND s.id = :id")
    Optional<StocktakeJpa> findByScope(
            @Param("org") UUID organizationId,
            @Param("store") UUID storeId,
            @Param("id") UUID id);
}
