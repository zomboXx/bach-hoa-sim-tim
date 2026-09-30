package vn.simtim.api.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.auth.domain.PasswordHasher;

/** Local demo bootstrap only. No credentials are loaded unless explicitly supplied. */
@Component
@Profile("demo")
public class DemoAccounts implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordHasher passwords;
    private final String password;
    public DemoAccounts(JdbcTemplate jdbc, PasswordHasher passwords,
            @Value("${SIMTIM_DEMO_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.isEmpty()) return;
        if (password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.length() < 12) {
            throw new IllegalArgumentException("Demo password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
        UUID org = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID store = UUID.fromString("10000000-0000-0000-0000-000000000002");
        int index = 101;
        UUID managerUserId = null;
        for (String role : new String[]{"SALES", "STOCK", "MANAGER", "ADMIN"}) {
            UUID id = UUID.fromString("20000000-0000-0000-0000-000000000" + index++);
            if ("MANAGER".equals(role)) managerUserId = id;
            jdbc.update("""
                    insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                    values(?,?,?,?,?,'ACTIVE') on conflict do nothing
                    """, id, org, role.toLowerCase(java.util.Locale.ROOT), passwords.hash(password), "Demo " + role);
            jdbc.update("""
                    insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                    select ?,u.id,r.id,?,now(),u.id from iam.users u join iam.roles r on r.organization_id=u.organization_id
                    where u.id=? and u.organization_id=? and r.code=? on conflict do nothing
                    """, org, store, id, org, role);
        }
        seedInventory(org, store, managerUserId);
    }

    /** Seed demo inventory: goods receipt → batches → balances → movements.
     *  Only runs when a password is supplied. Idempotent via ON CONFLICT DO NOTHING. */
    private void seedInventory(UUID org, UUID store, UUID actorUserId) {
        UUID supplierId   = UUID.fromString("10000000-0000-0000-0000-000000000061");
        UUID productRice  = UUID.fromString("10000000-0000-0000-0000-000000000041");
        UUID productApple = UUID.fromString("10000000-0000-0000-0000-000000000042");
        UUID receiptId    = UUID.fromString("10000000-0000-0000-0000-000000000081");
        UUID lineRiceId   = UUID.fromString("10000000-0000-0000-0000-000000000091");
        UUID lineAppleId  = UUID.fromString("10000000-0000-0000-0000-000000000092");
        UUID batchRiceId  = UUID.fromString("10000000-0000-0000-0000-0000000000a1");
        UUID batchAppleId = UUID.fromString("10000000-0000-0000-0000-0000000000a2");
        UUID balRiceId    = UUID.fromString("10000000-0000-0000-0000-0000000000b1");
        UUID balAppleId   = UUID.fromString("10000000-0000-0000-0000-0000000000b2");
        UUID movRiceId    = UUID.fromString("10000000-0000-0000-0000-0000000000c1");
        UUID movAppleId   = UUID.fromString("10000000-0000-0000-0000-0000000000c2");

        jdbc.update("""
                insert into inventory.goods_receipts
                    (id,organization_id,store_id,supplier_id,receipt_no,status,received_at,created_by,confirmed_by)
                values(?,?,?,?,'RCV-DEMO-001','CONFIRMED','2026-01-01T00:00:00Z',?,?)
                on conflict do nothing
                """, receiptId, org, store, supplierId, actorUserId, actorUserId);

        jdbc.update("""
                insert into inventory.goods_receipt_lines
                    (id,organization_id,store_id,receipt_id,product_id,
                     expected_quantity,delivered_quantity,accepted_quantity,rejected_quantity,unit_cost)
                values(?,?,?,?,?, 100,100,100,0,18000)
                on conflict do nothing
                """, lineRiceId, org, store, receiptId, productRice);

        jdbc.update("""
                insert into inventory.goods_receipt_lines
                    (id,organization_id,store_id,receipt_id,product_id,
                     expected_quantity,delivered_quantity,accepted_quantity,rejected_quantity,unit_cost,expiry_date)
                values(?,?,?,?,?, 50,50,50,0,28000,'2026-12-31')
                on conflict do nothing
                """, lineAppleId, org, store, receiptId, productApple);

        jdbc.update("""
                insert into inventory.product_batches
                    (id,organization_id,store_id,product_id,receipt_line_id,
                     internal_batch_code,received_date,expiry_date,status)
                values(?,?,?,?,?,'BATCH-RICE-001','2026-01-01',null,'AVAILABLE')
                on conflict do nothing
                """, batchRiceId, org, store, productRice, lineRiceId);

        jdbc.update("""
                insert into inventory.product_batches
                    (id,organization_id,store_id,product_id,receipt_line_id,
                     internal_batch_code,received_date,expiry_date,status)
                values(?,?,?,?,?,'BATCH-APPLE-001','2026-01-01','2026-12-31','AVAILABLE')
                on conflict do nothing
                """, batchAppleId, org, store, productApple, lineAppleId);

        jdbc.update("""
                insert into inventory.inventory_balances
                    (id,organization_id,store_id,product_batch_id,quantity_on_hand)
                values(?,?,?,?,100)
                on conflict do nothing
                """, balRiceId, org, store, batchRiceId);

        jdbc.update("""
                insert into inventory.inventory_balances
                    (id,organization_id,store_id,product_batch_id,quantity_on_hand)
                values(?,?,?,?,50)
                on conflict do nothing
                """, balAppleId, org, store, batchAppleId);

        jdbc.update("""
                insert into inventory.stock_movements
                    (id,organization_id,store_id,product_batch_id,movement_type,
                     quantity_delta,receipt_line_id,occurred_at,actor_user_id)
                values(?,?,?,?,'RECEIPT',100,?,'2026-01-01T00:00:00Z',?)
                on conflict do nothing
                """, movRiceId, org, store, batchRiceId, lineRiceId, actorUserId);

        jdbc.update("""
                insert into inventory.stock_movements
                    (id,organization_id,store_id,product_batch_id,movement_type,
                     quantity_delta,receipt_line_id,occurred_at,actor_user_id)
                values(?,?,?,?,'RECEIPT',50,?,'2026-01-01T00:00:00Z',?)
                on conflict do nothing
                """, movAppleId, org, store, batchAppleId, lineAppleId, actorUserId);
    }
}
