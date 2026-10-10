package vn.simtim.api.inventory.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "inventory", name = "stocktakes")
class StocktakeJpa {

    @Id UUID id;
    @Column(name = "organization_id", nullable = false) UUID organizationId;
    @Column(name = "store_id",        nullable = false) UUID storeId;
    @Column(name = "actor_id",        nullable = false) UUID actorId;
    @Column(nullable = false)                           String status;
    @Column(name = "opened_at",       nullable = false) Instant openedAt;
    @Column(name = "submitted_at")                      Instant submittedAt;

    StocktakeJpa() {}
}
