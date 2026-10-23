package vn.simtim.api.sale.infrastructure;

import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.inventory.application.SaleIssue;
import vn.simtim.api.sale.domain.*;

@Repository
class TransferRepositoryAdapter implements TransferRepository {
    private final JdbcTemplate jdbc;
    private final EntityManager entities;
    TransferRepositoryAdapter(JdbcTemplate jdbc, EntityManager entities) { this.jdbc=jdbc; this.entities=entities; }

    public Optional<TransferOrder> find(UUID id, boolean lock) {
        return jdbc.query("select * from sales.transfer_orders where id=?" + (lock ? " for update" : ""),
                this::map, id).stream().findFirst();
    }
    public Optional<TransferOrder> pending(UUID cart) {
        return jdbc.query("select * from sales.transfer_orders where cart_id=? and status='PENDING'",
                this::map, cart).stream().findFirst();
    }
    public Optional<TransferOrder> operation(PosCart.Scope scope, UUID op) {
        return jdbc.query("""
                select * from sales.transfer_orders where organization_id=? and store_id=? and owner_id=? and operation_id=?
                """, this::map, scope.organizationId(), scope.storeId(), scope.ownerId(), op).stream().findFirst();
    }
    public List<TransferOrder> list(UUID org, UUID store, UUID owner, int limit, int offset) {
        return jdbc.query("""
                select * from sales.transfer_orders where organization_id=? and store_id=?
                and (?::uuid is null or owner_id=?) order by created_at desc,id desc limit ? offset ?
                """, this::map, org, store, owner, owner, limit, offset);
    }
    public Summary summary(UUID org, UUID store, UUID owner) {
        return jdbc.queryForObject("""
                select count(*) filter(where status='PENDING'),count(*) filter(where status='COMPLETED'),
                count(*) filter(where status='CANCELLED'),count(*) filter(where status='EXPIRED'),
                coalesce(sum(amount) filter(where status='COMPLETED'),0)::bigint
                from sales.transfer_orders where organization_id=? and store_id=? and (?::uuid is null or owner_id=?)
                """, (rs,i)->new Summary(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5)),
                org, store, owner, owner);
    }
    public List<UUID> expired(Instant now) {
        return jdbc.queryForList("select id from sales.transfer_orders where status='PENDING' and expires_at<=? order by expires_at limit 100",
                UUID.class, Timestamp.from(now));
    }
    public void save(TransferOrder o) {
        entities.flush();
        jdbc.update("""
                insert into sales.transfer_orders(id,organization_id,store_id,owner_id,cart_id,invoice_id,operation_id,
                  cart_version,reference,amount,status,bank_bin,account_number,account_name,created_at,expires_at)
                values(?,?,?,?,?,?,?,?,?,?,'PENDING',?,?,?,?,?)
                """, o.id(),o.organizationId(),o.storeId(),o.ownerId(),o.cartId(),o.invoiceId(),o.operationId(),
                o.cartVersion(),o.reference(),o.amount(),o.bankBin(),o.accountNumber(),o.accountName(),
                Timestamp.from(o.createdAt()),Timestamp.from(o.expiresAt()));
    }
    public List<SaleIssue> allocations(TransferOrder o, UUID actor) {
        return jdbc.query("""
                select b.invoice_line_id,b.product_id,b.product_batch_id,b.quantity
                from sales.invoice_line_batches b join sales.invoice_lines l on l.id=b.invoice_line_id
                where l.organization_id=? and l.store_id=? and l.invoice_id=? order by b.product_id,b.product_batch_id,b.invoice_line_id
                """, (rs,i)->new SaleIssue(UUID.randomUUID(),rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),
                        rs.getObject(3,UUID.class),rs.getBigDecimal(4),actor),o.organizationId(),o.storeId(),o.invoiceId());
    }
    public boolean receiptUsed(TransferOrder o, String ref) {
        // Serialize confirmations of the same bank receipt even when they belong to different carts.
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?,0))", (rs,i)->0,
                o.organizationId()+"|"+o.bankBin()+"|"+o.accountNumber()+"|"+ref);
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from sales.transfer_orders where organization_id=? and bank_bin=?
                and account_number=? and bank_reference=? and status='COMPLETED')
                """, Boolean.class, o.organizationId(),o.bankBin(),o.accountNumber(),ref));
    }
    public void complete(TransferOrder o, UUID actor, String ref, Instant now) {
        jdbc.update("""
                update sales.invoices set status='COMPLETED',sold_at=?,paid_total=grand_total,version=version+1
                where id=? and status='DRAFT'
                """, Timestamp.from(now),o.invoiceId());
        jdbc.update("""
                insert into sales.payments(id,organization_id,store_id,invoice_id,method,status,amount,reference_code,paid_at)
                values(?,?,?,?,'TRANSFER','COMPLETED',?,?,?)
                """,UUID.randomUUID(),o.organizationId(),o.storeId(),o.invoiceId(),o.amount(),ref,Timestamp.from(now));
        resolve(o,"COMPLETED",actor,ref,"Nhân viên xác nhận đã nhận tiền",now);
    }
    public void release(TransferOrder o, String status, UUID actor, String note, Instant now) {
        jdbc.update("update sales.invoices set status='VOIDED',voided_by=?,void_reason=?,version=version+1 where id=? and status='DRAFT'",
                actor==null?o.ownerId():actor,note,o.invoiceId());
        resolve(o,status,actor,null,note,now);
    }
    private void resolve(TransferOrder o, String status, UUID actor, String ref, String note, Instant now) {
        jdbc.update("""
                update sales.transfer_orders set status=?,resolved_at=?,resolved_by=?,bank_reference=?,resolution_note=? where id=?
                """,status,Timestamp.from(now),actor,ref,note,o.id());
    }
    private TransferOrder map(ResultSet r, int index) throws SQLException {
        Timestamp resolved = r.getTimestamp("resolved_at");
        return new TransferOrder(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),
                r.getObject("store_id",UUID.class),r.getObject("owner_id",UUID.class),r.getObject("cart_id",UUID.class),
                r.getObject("invoice_id",UUID.class),r.getObject("operation_id",UUID.class),r.getLong("cart_version"),
                r.getString("reference"),r.getLong("amount"),r.getString("status"),r.getString("bank_bin"),
                r.getString("account_number"),r.getString("account_name"),r.getTimestamp("created_at").toInstant(),
                r.getTimestamp("expires_at").toInstant(),resolved==null?null:resolved.toInstant(),
                r.getObject("resolved_by",UUID.class),r.getString("bank_reference"),r.getString("resolution_note"));
    }
}
