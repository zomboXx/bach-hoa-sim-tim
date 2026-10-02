package vn.simtim.api.inventory.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBalanceJpaRepository extends JpaRepository<InventoryBalanceJpa, UUID> {
}
