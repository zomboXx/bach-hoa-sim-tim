package vn.simtim.api.inventory.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.inventory.application.*;

@Component
@Transactional(propagation = Propagation.MANDATORY)
class JdbcInventoryHoldAdapter implements InventoryHoldPort {
    private final NamedParameterJdbcTemplate jdbc;
    private final InventoryProductLocker products;
    private final Clock clock;

    JdbcInventoryHoldAdapter(NamedParameterJdbcTemplate jdbc, InventoryProductLocker products, Clock clock) {
        this.jdbc = jdbc;
        this.products = products;
        this.clock = clock;
    }

    public void reserve(FefoPlan plan) {
        for (var item : plan.allocations()) {
            int count = jdbc.update("""
                    update inventory.inventory_balances set reserved_quantity=reserved_quantity+:quantity,version=version+1
                    where organization_id=:org and store_id=:store and product_id=:product and batch_id=:batch
                      and on_hand_quantity-reserved_quantity>=:quantity
                    """, params(plan.scope(), item.productId(), item.batchId(), item.quantity()));
            requireUpdated(count);
        }
    }

    public void release(StoreScope scope, List<SaleIssue> allocations) {
        lockBalances(scope, allocations);
        for (var item : allocations) {
            requireUpdated(jdbc.update("""
                    update inventory.inventory_balances set reserved_quantity=reserved_quantity-:quantity,version=version+1
                    where organization_id=:org and store_id=:store and product_id=:product and batch_id=:batch
                      and reserved_quantity>=:quantity
                    """, params(scope, item.productId(), item.batchId(), item.quantity())));
        }
    }

    public void complete(StoreScope scope, List<SaleIssue> allocations) {
        var ids = allocations.stream().map(SaleIssue::productId).distinct().sorted().toList();
        if (!products.lockActiveProducts(scope.organizationId(), ids).equals(ids)) {
            throw new InventorySaleException("STOCK_CHANGED", "Sản phẩm đã ngừng bán; cần đối chiếu khoản tiền trước khi xử lý tiếp.");
        }
        lockBalances(scope, allocations);
        var today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        for (var item : allocations) {
            var p = params(scope, item.productId(), item.batchId(), item.quantity()).addValue("today", today);
            requireUpdated(jdbc.update("""
                    update inventory.inventory_balances b
                    set on_hand_quantity=b.on_hand_quantity-:quantity,
                        reserved_quantity=b.reserved_quantity-:quantity,version=b.version+1
                    where b.organization_id=:org and b.store_id=:store and b.product_id=:product and b.batch_id=:batch
                      and b.reserved_quantity>=:quantity and b.on_hand_quantity>=:quantity
                      and exists(select 1 from inventory.product_batches pb where pb.id=b.batch_id
                          and pb.status='AVAILABLE' and (pb.expiry_date is null or pb.expiry_date>=:today))
                    """, p));
            jdbc.update("""
                    insert into inventory.stock_movements(id,organization_id,store_id,product_id,batch_id,
                      movement_type,quantity_delta,reference_id,reference_type,occurred_at,recorded_by)
                    values(:id,:org,:store,:product,:batch,'SALE',-:quantity,:line,'INVOICE',:at,:actor)
                    """, p.addValue("id", item.movementId()).addValue("line", item.invoiceLineId())
                        .addValue("actor", item.actorId()).addValue("at", Timestamp.from(clock.instant())));
        }
    }

    private void lockBalances(StoreScope scope, List<SaleIssue> items) {
        jdbc.query("""
                select id from inventory.inventory_balances where organization_id=:org and store_id=:store
                and batch_id in (:batches) order by id for update
                """, Map.of("org", scope.organizationId(), "store", scope.storeId(),
                        "batches", items.stream().map(SaleIssue::batchId).distinct().toList()), (rs, i) -> rs.getObject(1));
    }

    private MapSqlParameterSource params(StoreScope scope, java.util.UUID product, java.util.UUID batch, java.math.BigDecimal quantity) {
        return new MapSqlParameterSource().addValue("org", scope.organizationId()).addValue("store", scope.storeId())
                .addValue("product", product).addValue("batch", batch).addValue("quantity", quantity);
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new InventorySaleException("STOCK_CHANGED", "Tồn hoặc lô giữ hàng không còn hợp lệ. Cần đối chiếu thanh toán.");
    }
}
