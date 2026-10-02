package vn.simtim.api.reports.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.simtim.api.reports.api.InventoryBalanceDto;
import vn.simtim.api.reports.api.RevenueReportResponse;
import vn.simtim.api.reports.domain.ReportsRepository;

@Repository
public class JdbcReportsRepository implements ReportsRepository {

    private final JdbcClient jdbc;

    public JdbcReportsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RevenueReportResponse getRevenue(UUID orgId, UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        var sql = """
            SELECT COALESCE(SUM(grand_total), 0) AS revenue,
                   COUNT(*) AS invoice_count
            FROM sales.invoices
            WHERE organization_id = :orgId
              AND store_id = :storeId
              AND status = 'COMPLETED'
              AND sold_at >= :from
              AND sold_at < :to
        """;

        return jdbc.sql(sql)
                .param("orgId", orgId)
                .param("storeId", storeId)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new RevenueReportResponse(
                        rs.getLong("revenue"),
                        rs.getLong("invoice_count")
                )).single();
    }

    @Override
    public List<InventoryBalanceDto> getInventory(UUID orgId, UUID storeId, LocalDate today) {
        var sql = """
            SELECT
                p.id AS product_id,
                p.sku,
                p.name,
                ib.quantity_on_hand,
                pb.expiry_date,
                CASE
                    WHEN pb.expiry_date IS NULL THEN 'NO_EXPIRY'
                    WHEN pb.expiry_date < :today THEN 'EXPIRED'
                    WHEN pb.expiry_date <= :nearExpiry THEN 'NEAR_EXPIRY'
                    ELSE 'VALID'
                END AS status
            FROM inventory.inventory_balances ib
            JOIN inventory.product_batches pb ON ib.product_batch_id = pb.id
            JOIN catalog.products p ON pb.product_id = p.id
            WHERE ib.organization_id = :orgId
              AND ib.store_id = :storeId
              AND pb.status != 'DEPLETED'
              AND pb.status != 'BLOCKED'
              AND ib.quantity_on_hand > 0
            ORDER BY p.name, pb.expiry_date
        """;

        return jdbc.sql(sql)
                .param("orgId", orgId)
                .param("storeId", storeId)
                .param("today", today)
                .param("nearExpiry", today.plusDays(7))
                .query((rs, rowNum) -> new InventoryBalanceDto(
                        rs.getObject("product_id", UUID.class),
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getBigDecimal("quantity_on_hand"),
                        rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null,
                        rs.getString("status")
                )).list();
    }
}
