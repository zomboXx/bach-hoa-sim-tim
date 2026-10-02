package vn.simtim.api.inventory.infrastructure;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.inventory.application.*;

/** PostgreSQL read adapter; mọi truy vấn đều bắt buộc organization/store scope. */
@Repository
class JdbcInventoryReadRepository implements InventoryReadRepository {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcInventoryReadRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InventoryPage<InventoryProductView> findProducts(
            UUID organizationId, UUID storeId, UUID productId, String productStatus,
            LocalDate businessDate, int page, int size) {
        Map<String, Object> params = baseParams(organizationId, storeId, page, size);
        params.put("productId", productId);
        params.put("status", productStatus);
        params.put("businessDate", businessDate);
        String where = """
                p.organization_id = :organizationId
                AND (CAST(:productId AS uuid) IS NULL OR p.id = :productId)
                AND (CAST(:status AS varchar) IS NULL OR p.status = :status)
                """;
        long total = count("SELECT count(*) FROM catalog.products p WHERE " + where, params);
        List<InventoryProductView> items = jdbc.query("""
                SELECT p.id, p.sku, p.name, p.status,
                       COALESCE(sum(ib.on_hand_quantity), 0) AS on_hand_quantity,
                       COALESCE(sum(CASE
                           WHEN pb.status = 'AVAILABLE'
                            AND (pb.expiry_date IS NULL OR pb.expiry_date >= :businessDate)
                            AND ib.on_hand_quantity > 0
                           THEN ib.on_hand_quantity ELSE 0 END), 0) AS available_quantity
                FROM catalog.products p
                LEFT JOIN inventory.inventory_balances ib
                  ON ib.organization_id = p.organization_id
                 AND ib.store_id = :storeId
                 AND ib.product_id = p.id
                LEFT JOIN inventory.product_batches pb
                  ON pb.organization_id = ib.organization_id
                 AND pb.id = ib.batch_id
                WHERE
                """ + where + """
                GROUP BY p.id, p.sku, p.name, p.status
                ORDER BY lower(p.sku), p.id
                LIMIT :size OFFSET :offset
                """, params, (rs, rowNum) -> new InventoryProductView(
                        rs.getObject("id", UUID.class), rs.getString("sku"),
                        rs.getString("name"), rs.getString("status"),
                        rs.getBigDecimal("on_hand_quantity"), rs.getBigDecimal("available_quantity"),
                        businessDate));
        return new InventoryPage<>(items, page, size, total);
    }

    @Override
    public InventoryPage<InventoryBatchView> findBatches(
            UUID organizationId, UUID storeId, UUID productId, String batchStatus,
            ExpiryStatus expiryStatus, LocalDate businessDate, int page, int size) {
        Map<String, Object> params = baseParams(organizationId, storeId, page, size);
        params.put("productId", productId);
        params.put("status", batchStatus);
        params.put("businessDate", businessDate);
        params.put("nearExpiryEnd", businessDate.plusDays(7));
        params.put("expiryStatus", expiryStatus == null ? null : expiryStatus.name());
        String expiryCase = """
                CASE
                    WHEN pb.expiry_date IS NULL THEN 'NO_EXPIRY'
                    WHEN pb.expiry_date < :businessDate THEN 'EXPIRED'
                    WHEN pb.expiry_date <= :nearExpiryEnd THEN 'NEAR_EXPIRY'
                    ELSE 'VALID'
                END
                """;
        String where = """
                pb.organization_id = :organizationId
                AND pb.store_id = :storeId
                AND (CAST(:productId AS uuid) IS NULL OR pb.product_id = :productId)
                AND (CAST(:status AS varchar) IS NULL OR pb.status = :status)
                AND (CAST(:expiryStatus AS varchar) IS NULL OR
                """ + expiryCase + " = :expiryStatus)";
        long total = count("SELECT count(*) FROM inventory.product_batches pb WHERE " + where, params);
        List<InventoryBatchView> items = jdbc.query("""
                SELECT pb.id, pb.product_id, pb.batch_number, pb.supplier_lot_number,
                       pb.status, pb.expiry_date, pb.received_date,
                       ib.on_hand_quantity,
                       CASE WHEN pb.status = 'AVAILABLE'
                              AND (pb.expiry_date IS NULL OR pb.expiry_date >= :businessDate)
                              AND ib.on_hand_quantity > 0
                            THEN ib.on_hand_quantity ELSE 0 END AS available_quantity,
                       """ + expiryCase + " AS expiry_status " + """
                FROM inventory.product_batches pb
                JOIN inventory.inventory_balances ib
                  ON ib.organization_id = pb.organization_id
                 AND ib.store_id = pb.store_id
                 AND ib.batch_id = pb.id
                WHERE
                """ + where + """
                ORDER BY pb.product_id, pb.expiry_date ASC NULLS LAST, pb.received_date, pb.id
                LIMIT :size OFFSET :offset
                """, params, (rs, rowNum) -> new InventoryBatchView(
                        rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                        rs.getString("batch_number"), rs.getString("supplier_lot_number"),
                        rs.getString("status"), ExpiryStatus.valueOf(rs.getString("expiry_status")),
                        nullableDate(rs.getDate("expiry_date")), rs.getDate("received_date").toLocalDate(),
                        rs.getBigDecimal("on_hand_quantity"), rs.getBigDecimal("available_quantity"),
                        businessDate));
        return new InventoryPage<>(items, page, size, total);
    }

    @Override
    public InventoryPage<InventoryMovementView> findMovements(
            UUID organizationId, UUID storeId, UUID productId, UUID batchId,
            String movementType, int page, int size) {
        Map<String, Object> params = baseParams(organizationId, storeId, page, size);
        params.put("productId", productId);
        params.put("batchId", batchId);
        params.put("type", movementType);
        String where = """
                sm.organization_id = :organizationId
                AND sm.store_id = :storeId
                AND (CAST(:productId AS uuid) IS NULL OR sm.product_id = :productId)
                AND (CAST(:batchId AS uuid) IS NULL OR sm.batch_id = :batchId)
                AND (CAST(:type AS varchar) IS NULL OR sm.movement_type = :type)
                """;
        long total = count("SELECT count(*) FROM inventory.stock_movements sm WHERE " + where, params);
        List<InventoryMovementView> items = jdbc.query("""
                SELECT sm.id, sm.product_id, sm.batch_id, sm.movement_type,
                       sm.quantity_delta, sm.reference_type, sm.reference_id,
                       sm.occurred_at, sm.recorded_by
                FROM inventory.stock_movements sm
                WHERE
                """ + where + """
                ORDER BY sm.occurred_at DESC, sm.id DESC
                LIMIT :size OFFSET :offset
                """, params, (rs, rowNum) -> new InventoryMovementView(
                        rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                        rs.getObject("batch_id", UUID.class), rs.getString("movement_type"),
                        rs.getBigDecimal("quantity_delta"), rs.getString("reference_type"),
                        rs.getObject("reference_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getObject("recorded_by", UUID.class)));
        return new InventoryPage<>(items, page, size, total);
    }

    private Map<String, Object> baseParams(
            UUID organizationId, UUID storeId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("organizationId", organizationId);
        params.put("storeId", storeId);
        params.put("size", size);
        params.put("offset", Math.multiplyExact((long) page, size));
        return params;
    }

    private long count(String sql, Map<String, Object> params) {
        Long result = jdbc.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    private LocalDate nullableDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
