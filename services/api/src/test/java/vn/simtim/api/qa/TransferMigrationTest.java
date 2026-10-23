package vn.simtim.api.qa;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class TransferMigrationTest {
    @Test void upgradePreservesCompletedBankReceipt() {
        try(var pg=new PostgreSQLContainer<>("postgres:17-alpine").withDatabaseName("transfer_upgrade")) {
            pg.start();
            var config=Flyway.configure().dataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword())
                    .locations("classpath:db/migration");
            config.target("19").load().migrate();
            var jdbc=new JdbcTemplate(new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword()));
            UUID org=UUID.randomUUID(),store=UUID.randomUUID(),user=UUID.randomUUID(),invoice=UUID.randomUUID(),cart=UUID.randomUUID(),order=UUID.randomUUID();
            jdbc.update("insert into core.organizations values(?,'UPGRADE','Fixture','ACTIVE')",org);
            jdbc.update("insert into core.stores(id,organization_id,code,name,status) values(?,?,'UPGRADE','Fixture','ACTIVE')",store,org);
            jdbc.update("insert into iam.users(id,organization_id,username,password_hash,full_name,status) values(?,?,'fixture','unused','Fixture','ACTIVE')",user,org);
            jdbc.update("insert into sales.invoices(id,organization_id,store_id,invoice_no,status,sold_by,sold_at,subtotal,grand_total,paid_total) values(?,?,?,'OLD','COMPLETED',?,now(),25000,25000,25000)",invoice,org,store,user);
            jdbc.update("insert into sales.pos_carts(id,organization_id,store_id,owner_id) values(?,?,?,?)",cart,org,store,user);
            jdbc.update("""
                insert into sales.transfer_orders(id,organization_id,store_id,owner_id,cart_id,invoice_id,operation_id,
                cart_version,reference,amount,status,bank_bin,account_number,account_name,created_at,expires_at,resolved_at,resolved_by,bank_reference)
                values(?,?,?,?,?,?,?,0,'OLD',25000,'COMPLETED','970436','0000000000','TEST ONLY',now(),now()+interval '10 minutes',now(),?,'OLD-RECEIPT')
                """,order,org,store,user,cart,invoice,UUID.randomUUID(),user);
            config.target("latest").load().migrate();
            assertThat(jdbc.queryForMap("select confirmation_method,bank_reference,amount from sales.transfer_orders where id=?",order))
                    .containsEntry("confirmation_method","BANK_RECEIPT").containsEntry("bank_reference","OLD-RECEIPT").containsEntry("amount",25000L);
            UUID nextInvoice=UUID.randomUUID(),nextOrder=UUID.randomUUID();
            jdbc.update("insert into sales.invoices(id,organization_id,store_id,invoice_no,status,sold_by,sold_at) values(?,?,?,'NEXT','DRAFT',?,now())",nextInvoice,org,store,user);
            jdbc.update("""
                insert into sales.transfer_orders(id,organization_id,store_id,owner_id,cart_id,invoice_id,operation_id,
                cart_version,reference,amount,status,bank_bin,account_number,account_name,created_at,expires_at)
                values(?,?,?,?,?,?,?,0,'NEXT',25000,'PENDING','970436','0000000000','TEST ONLY',now(),now()+interval '10 minutes')
                """,nextOrder,org,store,user,cart,nextInvoice,UUID.randomUUID());
            jdbc.update("update sales.transfer_orders set status='COMPLETED',resolved_by=?,resolved_at=now(),bank_reference='NEXT-RECEIPT' where id=?",user,nextOrder);
            assertThat(jdbc.queryForObject("select confirmation_method from sales.transfer_orders where id=?",String.class,nextOrder)).isEqualTo("BANK_RECEIPT");
        }
    }
}
