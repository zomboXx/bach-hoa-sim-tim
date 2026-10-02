package vn.simtim.api.inventory.infrastructure;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.*;

/** PostgreSQL adapter cho public sale port; transaction luôn do consumer SAL-01 sở hữu. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
class JdbcInventorySaleAdapter implements InventorySalePort {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NamedParameterJdbcTemplate jdbc;
    private final InventoryProductLocker productLocker;
    private final Clock clock;

    JdbcInventorySaleAdapter(
            NamedParameterJdbcTemplate jdbc,
            InventoryProductLocker productLocker,
            Clock clock) {
        this.jdbc = jdbc;
        this.productLocker = productLocker;
        this.clock = clock;
    }

    @Override
    public FefoPlan planForCheckout(StoreScope scope, List<StockDemand> demands) {
        LinkedHashMap<UUID, BigDecimal> quantities = normalizeDemands(demands);
        List<UUID> productIds = quantities.keySet().stream().sorted().toList();

        List<UUID> lockedProducts = productLocker.lockActiveProducts(
                scope.organizationId(), productIds);
        if (!lockedProducts.equals(productIds)) {
            throw new InventorySaleException(
                    "NOT_FOUND", "Sản phẩm không tồn tại hoặc không hoạt động");
        }

        lockBalances(scope, productIds);
        Instant capturedAt = clock.instant();
        LocalDate businessDate = LocalDate.ofInstant(capturedAt, BUSINESS_ZONE);
        Map<UUID, List<Candidate>> candidates = loadCandidates(scope, productIds, businessDate);

        List<FefoAllocation> allocations = new ArrayList<>();
        for (UUID productId : productIds) {
            BigDecimal remaining = quantities.get(productId);
            for (Candidate candidate : candidates.getOrDefault(productId, List.of())) {
                if (remaining.signum() == 0) {
                    break;
                }
                BigDecimal allocated = remaining.min(candidate.quantity());
                allocations.add(new FefoAllocation(
                        productId, candidate.batchId(), allocated,
                        candidate.expiryDate(), candidate.receivedDate()));
                remaining = remaining.subtract(allocated);
            }
            if (remaining.signum() > 0) {
                throw new InventorySaleException(
                        "INSUFFICIENT_STOCK",
                        "Không đủ tồn khả dụng cho sản phẩm " + productId);
            }
        }
        return new FefoPlan(scope, capturedAt, businessDate, allocations);
    }

    @Override
    public void postSale(FefoPlan plan, List<SaleIssue> issuedAllocations) {
        Map<AllocationKey, BigDecimal> planned = sumPlan(plan.allocations());
        Map<AllocationKey, BigDecimal> issued = sumIssues(issuedAllocations);
        if (!sameQuantities(planned, issued)) {
            throw new InventorySaleException(
                    "INVALID_REQUEST", "Phân bổ invoice không khớp FEFO plan đã khóa");
        }

        for (SaleIssue issue : issuedAllocations) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("organizationId", plan.scope().organizationId())
                    .addValue("storeId", plan.scope().storeId())
                    .addValue("productId", issue.productId())
                    .addValue("batchId", issue.batchId())
                    .addValue("quantity", issue.quantity());
            int updated = jdbc.update("""
                    UPDATE inventory.inventory_balances
                    SET on_hand_quantity = on_hand_quantity - :quantity,
                        version = version + 1
                    WHERE organization_id = :organizationId
                      AND store_id = :storeId
                      AND product_id = :productId
                      AND batch_id = :batchId
                      AND on_hand_quantity >= :quantity
                    """, params);
            if (updated != 1) {
                throw new InventorySaleException(
                        "INSUFFICIENT_STOCK", "Tồn kho đã thay đổi trong checkout");
            }
            params.addValue("movementId", issue.movementId())
                    .addValue("invoiceLineId", issue.invoiceLineId())
                    .addValue("actorId", issue.actorId())
                    .addValue("occurredAt", Timestamp.from(plan.capturedAt()));
            jdbc.update("""
                    INSERT INTO inventory.stock_movements(
                        id, organization_id, store_id, product_id, batch_id,
                        movement_type, quantity_delta, reference_id, reference_type,
                        occurred_at, recorded_by)
                    VALUES (
                        :movementId, :organizationId, :storeId, :productId, :batchId,
                        'SALE', -:quantity, :invoiceLineId, 'INVOICE',
                        :occurredAt, :actorId)
                    """, params);
        }
    }

    private LinkedHashMap<UUID, BigDecimal> normalizeDemands(List<StockDemand> demands) {
        if (demands == null || demands.isEmpty()) {
            throw new InventorySaleException("INVALID_REQUEST", "Checkout phải có nhu cầu tồn");
        }
        Map<UUID, BigDecimal> merged = new HashMap<>();
        for (StockDemand demand : demands) {
            merged.merge(demand.productId(), demand.quantity(), BigDecimal::add);
        }
        LinkedHashMap<UUID, BigDecimal> sorted = new LinkedHashMap<>();
        merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private void lockBalances(StoreScope scope, List<UUID> productIds) {
        jdbc.query("""
                SELECT id
                FROM inventory.inventory_balances
                WHERE organization_id = :organizationId
                  AND store_id = :storeId
                  AND product_id IN (:productIds)
                ORDER BY id
                FOR UPDATE
                """, Map.of(
                        "organizationId", scope.organizationId(),
                        "storeId", scope.storeId(),
                        "productIds", productIds),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    private Map<UUID, List<Candidate>> loadCandidates(
            StoreScope scope, List<UUID> productIds, LocalDate businessDate) {
        Map<String, Object> params = Map.of(
                "organizationId", scope.organizationId(),
                "storeId", scope.storeId(),
                "productIds", productIds,
                "businessDate", businessDate);
        List<Candidate> rows = jdbc.query("""
                SELECT pb.product_id, pb.id AS batch_id, ib.on_hand_quantity,
                       pb.expiry_date, pb.received_date
                FROM inventory.inventory_balances ib
                JOIN inventory.product_batches pb
                  ON pb.organization_id = ib.organization_id
                 AND pb.store_id = ib.store_id
                 AND pb.product_id = ib.product_id
                 AND pb.id = ib.batch_id
                WHERE ib.organization_id = :organizationId
                  AND ib.store_id = :storeId
                  AND ib.product_id IN (:productIds)
                  AND ib.on_hand_quantity > 0
                  AND pb.status = 'AVAILABLE'
                  AND (pb.expiry_date IS NULL OR pb.expiry_date >= :businessDate)
                ORDER BY pb.product_id, pb.expiry_date ASC NULLS LAST,
                         pb.received_date, pb.id
                """, params, (rs, rowNum) -> new Candidate(
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("batch_id", UUID.class),
                        rs.getBigDecimal("on_hand_quantity"),
                        nullableDate(rs.getDate("expiry_date")),
                        rs.getDate("received_date").toLocalDate()));
        Map<UUID, List<Candidate>> byProduct = new HashMap<>();
        for (Candidate row : rows) {
            byProduct.computeIfAbsent(row.productId(), ignored -> new ArrayList<>()).add(row);
        }
        return byProduct;
    }

    private Map<AllocationKey, BigDecimal> sumPlan(List<FefoAllocation> allocations) {
        Map<AllocationKey, BigDecimal> result = new HashMap<>();
        for (FefoAllocation allocation : allocations) {
            result.merge(new AllocationKey(allocation.productId(), allocation.batchId()),
                    allocation.quantity(), BigDecimal::add);
        }
        return result;
    }

    private Map<AllocationKey, BigDecimal> sumIssues(List<SaleIssue> issues) {
        if (issues == null) {
            throw new InventorySaleException("INVALID_REQUEST", "Thiếu phân bổ invoice");
        }
        Map<AllocationKey, BigDecimal> result = new HashMap<>();
        for (SaleIssue issue : issues) {
            result.merge(new AllocationKey(issue.productId(), issue.batchId()),
                    issue.quantity(), BigDecimal::add);
        }
        return result;
    }

    private boolean sameQuantities(
            Map<AllocationKey, BigDecimal> planned,
            Map<AllocationKey, BigDecimal> issued) {
        if (!planned.keySet().equals(issued.keySet())) {
            return false;
        }
        return planned.entrySet().stream()
                .allMatch(entry -> entry.getValue().compareTo(issued.get(entry.getKey())) == 0);
    }

    private LocalDate nullableDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private record Candidate(
            UUID productId,
            UUID batchId,
            BigDecimal quantity,
            LocalDate expiryDate,
            LocalDate receivedDate) {}

    private record AllocationKey(UUID productId, UUID batchId) {}
}
