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
}
