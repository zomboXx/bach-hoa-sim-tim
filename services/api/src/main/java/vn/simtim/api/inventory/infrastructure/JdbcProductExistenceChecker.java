package vn.simtim.api.inventory.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.simtim.api.inventory.application.ProductExistenceChecker;

/** Kiểm tra sản phẩm ACTIVE bằng JDBC để không phụ thuộc vào catalog JPA entity. */
@Component
class JdbcProductExistenceChecker implements ProductExistenceChecker {

    private final JdbcTemplate jdbc;

    JdbcProductExistenceChecker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isActive(UUID organizationId, UUID productId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM catalog.products WHERE organization_id=? AND id=? AND status='ACTIVE'",
                Integer.class, organizationId, productId);
        return count != null && count > 0;
    }

    @Override
    public int getPrecisionScale(UUID organizationId, UUID productId) {
        var list = jdbc.queryForList("""
                SELECT u.precision_scale
                FROM catalog.products p
                JOIN catalog.units u ON p.organization_id = u.organization_id AND p.base_unit_id = u.id
                WHERE p.organization_id = ? AND p.id = ?
                """, Integer.class, organizationId, productId);
        if (list.isEmpty() || list.getFirst() == null) {
            return 3;
        }
        return list.getFirst();
    }
}
