package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.sale.domain.PosCart;
import vn.simtim.api.sale.domain.PosCartRepository;

@Repository
public class PosCartRepositoryAdapter implements PosCartRepository {
    private final JdbcTemplate jdbc;
    private final EntityManager entities;

    public PosCartRepositoryAdapter(JdbcTemplate jdbc, EntityManager entities) {
        this.jdbc = jdbc;
        this.entities = entities;
    }

    @Override
    public PosCart read(PosCart.Scope scope, boolean exclusive) {
        var ids = jdbc.queryForList("""
                select id from sales.pos_carts where organization_id=? and store_id=? and owner_id=?
                """, UUID.class, scope.organizationId(), scope.storeId(), scope.ownerId());
        if (ids.isEmpty()) {
            jdbc.update("""
                    insert into sales.pos_carts(id,organization_id,store_id,owner_id) values(?,?,?,?)
                    on conflict (organization_id,store_id,owner_id) do nothing
                    """, UUID.randomUUID(), scope.organizationId(), scope.storeId(), scope.ownerId());
        }
        String locking = exclusive ? " for update" : " for share";
        return jdbc.queryForObject("""
                select id,version,generation from sales.pos_carts
                where organization_id=? and store_id=? and owner_id=?
                """ + locking, (rs, row) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    List<PosCart.Line> lines = jdbc.query("""
                            select l.product_id,p.name,l.quantity from sales.pos_cart_lines l
                            join catalog.products p on p.id=l.product_id where l.cart_id=? order by p.id
                            """, (line, index) -> new PosCart.Line(line.getObject(1, UUID.class),
                                    line.getString(2), line.getBigDecimal(3)), id);
                    return new PosCart(id, rs.getLong("version"), rs.getLong("generation"), lines);
                }, scope.organizationId(), scope.storeId(), scope.ownerId());
    }

    @Override
    public Optional<PosCart.Operation> operation(UUID cartId, UUID operationId) {
        return jdbc.query("""
                select payload_hash,invoice_id from sales.pos_cart_operations where cart_id=? and operation_id=?
                """, (rs, row) -> new PosCart.Operation(rs.getString(1), rs.getObject(2, UUID.class)),
                cartId, operationId).stream().findFirst();
    }

    @Override
    public void setQuantity(UUID cartId, UUID productId, BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            jdbc.update("delete from sales.pos_cart_lines where cart_id=? and product_id=?", cartId, productId);
        } else {
            jdbc.update("""
                    insert into sales.pos_cart_lines(cart_id,product_id,quantity) values(?,?,?)
                    on conflict (cart_id,product_id) do update set quantity=excluded.quantity
                    """, cartId, productId, quantity);
        }
    }

    @Override
    public void advance(UUID cartId, boolean checkout) {
        if (checkout) jdbc.update("delete from sales.pos_cart_lines where cart_id=?", cartId);
        jdbc.update("""
                update sales.pos_carts set version=version+1,generation=generation+? where id=?
                """, checkout ? 1 : 0, cartId);
    }

    @Override
    public void record(UUID cartId, UUID operationId, String hash, UUID invoiceId) {
        // Invoice writes use JPA. Flush inside the same transaction before the JDBC FK references them.
        if (invoiceId != null) entities.flush();
        jdbc.update("""
                insert into sales.pos_cart_operations(cart_id,operation_id,payload_hash,invoice_id) values(?,?,?,?)
                """, cartId, operationId, hash, invoiceId);
    }
}
