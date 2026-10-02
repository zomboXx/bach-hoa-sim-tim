package vn.simtim.api.inventory.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vn.simtim.api.inventory.application.InventoryProductLocker;

@Component
class JdbcInventoryProductLocker implements InventoryProductLocker {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcInventoryProductLocker(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> lockActiveProducts(UUID organizationId, List<UUID> sortedProductIds) {
        if (sortedProductIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                SELECT id
                FROM catalog.products
                WHERE organization_id = :organizationId
                  AND status = 'ACTIVE'
                  AND id IN (:productIds)
                ORDER BY id
                FOR UPDATE
                """, Map.of("organizationId", organizationId, "productIds", sortedProductIds),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
    }
}
