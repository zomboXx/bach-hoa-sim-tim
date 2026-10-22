package vn.simtim.api.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.simtim.api.auth.domain.PasswordHasher;

/**
 * Manual transfer cross-module gate, using a disposable beneficiary and PostgreSQL database.
 *
 * <p>The HTTP responses drive the workflow, while PostgreSQL queries provide the oracle for
 * transactionality, ledger traceability and report reconciliation. The database is disposable and
 * contains no operational data.</p>
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "TRANSFER_ENABLED=true","TRANSFER_BANK_BIN=970436","TRANSFER_ACCOUNT_NUMBER=1234567890",
    "TRANSFER_ACCOUNT_NAME=TEST BENEFICIARY",
    "TRANSFER_ORGANIZATION_ID=10000000-0000-0000-0000-000000000001",
    "TRANSFER_STORE_ID=10000000-0000-0000-0000-000000000002",
    "TRANSFER_EXPIRY_INTERVAL_MS=3600000"
})
class ManualTransferE2eTest {

    protected static final UUID ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    protected static final UUID STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    protected static final UUID SUPPLIER_ID = UUID.fromString("10000000-0000-0000-0000-000000000061");
    protected static final UUID RICE_ID = UUID.fromString("10000000-0000-0000-0000-000000000041");
    protected static final UUID APPLE_ID = UUID.fromString("10000000-0000-0000-0000-000000000042");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PASSWORD = "qa02-disposable-password";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine").withDatabaseName("simtim_transfer_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected TestRestTemplate http;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordHasher passwords;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    protected String salesToken;
    protected String stockToken;
    protected String managerToken;
    protected String adminToken;

    @BeforeAll
    void createActors() {
        salesToken = createActor("SALES");
        stockToken = createActor("STOCK");
        managerToken = createActor("MANAGER");
        adminToken = createActor("ADMIN");
    }

    @BeforeEach
    void resetBusinessData() {
        jdbc.update("delete from inventory.stocktake_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.stocktakes where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.processed_operations where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.transfer_orders where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.pos_carts where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.stock_movements where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.payments where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoice_line_batches where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoice_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoices where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.inventory_balances where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.product_batches where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.goods_receipt_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.goods_receipts where organization_id=?", ORG_ID);
        jdbc.update("delete from audit.audit_logs where organization_id=?", ORG_ID);
        jdbc.update("update sales.promotions set status='INACTIVE' where organization_id=?", ORG_ID);
    }

    @AfterAll
    void cleanup() {
        try {
            resetBusinessData();
            for (UUID userId : fixtureUsers) {
                jdbc.update("delete from iam.auth_sessions where user_id=?", userId);
                jdbc.update("delete from iam.user_roles where user_id=?", userId);
                jdbc.update("delete from iam.users where id=?", userId);
            }
        } finally {
            if (POSTGRES.isRunning()) {
                POSTGRES.stop();
            }
        }
    }


    @Test
    void holdsCartAndStockWithoutRecordingRevenueThenConfirmsExactlyOnce() throws Exception {
        receive(RICE_ID,"3","3","0",null);
        var order=begin();
        String id=order.get("id").toString();
        assertThat(order).containsEntry("status","PENDING").containsEntry("amount",25000);
        assertThat(order.get("qrPayload").toString()).contains(order.get("reference").toString());
        assertThat(body(get("/api/v1/sales/cart",salesToken))).containsEntry("pendingTransferId",id);
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("3");
        assertThat(held()).isEqualByComparingTo("1");
        assertThat(count("select count(*) from sales.payments")).isZero();
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'")).isZero();
        assertThat((List<?>)get("/api/v1/sales/invoices",salesToken).getBody()).isEmpty();
        assertThat(post("/api/v1/sales/cart/operations",Map.of("operationId",UUID.randomUUID(),"generation",0,"productId",RICE_ID,"delta","1"),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/api/v1/sales/checkout",Map.of("items",List.of(Map.of("productId",RICE_ID,"quantity","1")),"cashAmount",25000),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var latch=new CountDownLatch(1);
        try(var workers=Executors.newFixedThreadPool(2)) {
            var a=workers.submit(()->{ latch.await(); return confirm(id,"BANK001",salesToken); });
            var b=workers.submit(()->{ latch.await(); return confirm(id,"BANK001",managerToken); });
            latch.countDown();
            assertThat(body(a.get(15,TimeUnit.SECONDS))).containsEntry("status","COMPLETED");
            assertThat(body(b.get(15,TimeUnit.SECONDS))).containsEntry("status","COMPLETED");
        }
        assertThat(confirm(id,"BANK001",salesToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("2");
        assertThat(held()).isEqualByComparingTo("0");
        assertThat(count("select count(*) from sales.payments where method='TRANSFER'")).isEqualTo(1);
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'")).isEqualTo(1);
        var stored=body(get("/api/v1/sales/transfers/"+id,salesToken));
        assertThat(stored.get("resolvedBy")).isNotNull();
        assertThat(stored).containsEntry("bankReference","BANK001").containsEntry("qrPayload",null);
        var history=body(get("/api/v1/sales/transfers",salesToken));
        assertThat(((Map<?,?>)history.get("summary")).get("receivedAmount")).isEqualTo(25000);
        var cart=body(get("/api/v1/sales/cart",salesToken));
        assertThat((List<?>)cart.get("items")).isEmpty();
        assertThat(cart.get("generation")).isEqualTo(1);
    }

    @Test
    void creationRetriesRecoverPersistedOrderAndConflictingPayloadIsRejected() {
        receive(RICE_ID,"3","3","0",null);
        adjust();
        var request=Map.of("operationId",UUID.randomUUID(),"cartVersion",1,"expectedTotal",25000);
        var first=body(post("/api/v1/sales/transfers",request,salesToken));
        assertThat(body(post("/api/v1/sales/transfers",request,salesToken)).get("id")).isEqualTo(first.get("id"));
        var changed=new java.util.HashMap<String,Object>(request); changed.put("expectedTotal",1);
        assertThat(body(post("/api/v1/sales/transfers",changed,salesToken))).containsEntry("code","IDEMPOTENCY_KEY_REUSED");
        assertThat(count("select count(*) from sales.transfer_orders")).isEqualTo(1);
        assertThat(held()).isEqualByComparingTo("1");
    }

    @Test
    void cancelAndExpiryReleaseHoldsAndKeepHistoryWithoutSale() {
        receive(RICE_ID,"2","2","0",null);
        String id=begin().get("id").toString();
        assertThat(body(post("/api/v1/sales/transfers/"+id+"/cancel",Map.of("reason","Khách đổi ý"),salesToken)))
                .containsEntry("status","CANCELLED");
        assertThat(held()).isEqualByComparingTo("0");
        long version=((Number)body(get("/api/v1/sales/cart",salesToken)).get("version")).longValue();
        String second=body(post("/api/v1/sales/transfers",Map.of("operationId",UUID.randomUUID(),"cartVersion",version,"expectedTotal",25000),salesToken)).get("id").toString();
        jdbc.update("update sales.transfer_orders set created_at=now()-interval '20 minutes',expires_at=now()-interval '1 minute' where id=?",UUID.fromString(second));
        assertThat(body(confirm(second,"LATE001",salesToken))).containsEntry("status","EXPIRED");
        assertThat(held()).isEqualByComparingTo("0");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("2");
        assertThat(count("select count(*) from sales.payments")).isZero();
        assertThat(count("select count(*) from sales.invoices where status='VOIDED'")).isEqualTo(2);
        assertThat(count("select count(*) from sales.transfer_orders")).isEqualTo(2);
    }

    @Test
    void enforcesActorStoreAndRoleScopesAndRejectsFalseConfirmation() {
        receive(RICE_ID,"3","3","0",null);
        String id=begin().get("id").toString();
        String other=createActor("SALES");
        assertThat(get("/api/v1/sales/transfers/"+id,other).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(confirm(id,"STOLEN001",other).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(confirm(id,"STOCK001",stockToken).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/v1/sales/transfers/"+id,null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var wrong=headers(salesToken); wrong.set("X-Store-Id",UUID.randomUUID().toString());
        assertThat(http.exchange("/api/v1/sales/transfers/"+id,HttpMethod.GET,new HttpEntity<>(wrong),Object.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/v1/sales/transfers/"+id+"/confirm",Map.of("receivedAmount",1,"bankReference","BAD001","checkedBankReceipt",true),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(post("/api/v1/sales/transfers/"+id+"/confirm",Map.of("receivedAmount",25000,"bankReference","BAD001","checkedBankReceipt",false),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(held()).isEqualByComparingTo("1");
        assertThat(count("select count(*) from sales.payments")).isZero();
    }

    @Test
    void anotherSaleCannotConsumeHeldStockAndReusedBankReceiptCannotPayAgain() {
        receive(RICE_ID,"1","1","0",null);
        String id=begin().get("id").toString();
        assertThat(post("/api/v1/sales/checkout",Map.of("items",List.of(Map.of("productId",RICE_ID,"quantity","1")),"cashAmount",25000),managerToken)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirm(id,"RECEIPT001",salesToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        receive(RICE_ID,"1","1","0",null);
        String second=begin().get("id").toString();
        assertThat(body(confirm(second,"receipt001",salesToken))).containsEntry("code","BANK_REFERENCE_REUSED");
        assertThat(held()).isEqualByComparingTo("1");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("1");
        assertThat(count("select count(*) from sales.payments")).isEqualTo(1);
    }

    @Test
    void priceChangeAndBatchBlockRollbackWithoutPartialInvoiceOrMovement() {
        receive(RICE_ID,"3","3","0",null);
        adjust();
        assertThat(post("/api/v1/sales/transfers",Map.of("operationId",UUID.randomUUID(),"cartVersion",1,"expectedTotal",1),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(count("select count(*) from sales.invoices")).isZero();
        assertThat(held()).isEqualByComparingTo("0");
        String id=body(post("/api/v1/sales/transfers",Map.of("operationId",UUID.randomUUID(),"cartVersion",1,"expectedTotal",25000),salesToken)).get("id").toString();
        jdbc.update("update inventory.product_batches set status='BLOCKED' where product_id=?",RICE_ID);
        assertThat(confirm(id,"BLOCKED001",salesToken).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(held()).isEqualByComparingTo("1");
        assertThat(count("select count(*) from sales.payments")).isZero();
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'")).isZero();
        assertThat(body(get("/api/v1/sales/transfers/"+id,salesToken))).containsEntry("status","PENDING");
    }

    @Test
    void secondAllocationFailureRollsBackFirstAllocationMovementAndPayment() {
        receive(RICE_ID,"2","2","0",null);
        receive(APPLE_ID,"2","2","0",null);
        adjust();
        assertThat(post("/api/v1/sales/cart/operations",Map.of("operationId",UUID.randomUUID(),"generation",0,"productId",APPLE_ID,"delta","1"),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        String id=body(post("/api/v1/sales/transfers",Map.of("operationId",UUID.randomUUID(),"cartVersion",2,"expectedTotal",65000),salesToken)).get("id").toString();
        jdbc.update("update inventory.product_batches set status='BLOCKED' where product_id=?",APPLE_ID);
        assertThat(post("/api/v1/sales/transfers/"+id+"/confirm",Map.of("receivedAmount",65000,"bankReference","PARTIAL001","checkedBankReceipt",true),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("2");
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("2");
        assertThat(held()).isEqualByComparingTo("2");
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'")).isZero();
        assertThat(count("select count(*) from sales.payments")).isZero();
    }

    @Test
    void stocktakeCannotReducePhysicalBalanceBelowReservedQuantity() {
        receive(RICE_ID,"3","3","0",null);
        begin();
        var balance=jdbc.queryForMap("select batch_id,version from inventory.inventory_balances where product_id=?",RICE_ID);
        String stocktakeId=body(post("/api/v1/inventory/stocktakes",Map.of(),stockToken)).get("id").toString();
        assertThat(post("/api/v1/inventory/stocktakes/"+stocktakeId+"/counts",Map.of("clientOperationId",UUID.randomUUID(),
                "batchId",balance.get("batch_id"),"baseVersion",balance.get("version"),"actualQuantity","0","note","Kiểm tra giữ hàng"),stockToken)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(post("/api/v1/inventory/stocktakes/"+stocktakeId+"/approve",Map.of(),managerToken)))
                .containsEntry("code","STOCK_RESERVED");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("3");
        assertThat(held()).isEqualByComparingTo("1");
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='STOCKTAKE_ADJUSTMENT'")).isZero();
    }

    private void adjust() {
        var cart=body(get("/api/v1/sales/cart",salesToken));
        assertThat(post("/api/v1/sales/cart/operations",Map.of("operationId",UUID.randomUUID(),"generation",cart.get("generation"),"productId",RICE_ID,"delta","1"),salesToken)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    private Map<String,Object> begin() {
        adjust();
        var cart=body(get("/api/v1/sales/cart",salesToken));
        var response=post("/api/v1/sales/transfers",Map.of("operationId",UUID.randomUUID(),"cartVersion",cart.get("version"),"expectedTotal",25000),salesToken);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body(response);
    }
    private ResponseEntity<Object> confirm(String id,String ref,String token) {
        return post("/api/v1/sales/transfers/"+id+"/confirm",Map.of("receivedAmount",25000,"bankReference",ref,"checkedBankReceipt",true),token);
    }
    private BigDecimal held() { return jdbc.queryForObject("select coalesce(sum(reserved_quantity),0) from inventory.inventory_balances",BigDecimal.class); }
    protected UUID receive(UUID productId, String delivered, String accepted, String rejected, String reason) {
        UUID clientOperationId = UUID.randomUUID();
        var line = new java.util.LinkedHashMap<String, Object>();
        line.put("productId", productId);
        line.put("expectedQuantity", delivered);
        line.put("deliveredQuantity", delivered);
        line.put("acceptedQuantity", accepted);
        line.put("rejectedQuantity", rejected);
        line.put("unitCost", 10000);
        line.put("expiryDate", LocalDate.now(VN_ZONE).plusDays(30).toString());
        if (reason != null) {
            line.put("discrepancyReason", reason);
        }
        Map<String, Object> request = Map.of(
                "supplierId", SUPPLIER_ID,
                "clientOperationId", clientOperationId,
                "lines", List.of(line));
        HttpHeaders headers = headers(stockToken);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<Object> response = http.exchange(
                "/api/v1/inventory/receipts", HttpMethod.POST,
                new HttpEntity<>(request, headers), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(body(response).get("id").toString());
    }

    protected String createActor(String role) {
        UUID userId = UUID.randomUUID();
        fixtureUsers.add(userId);
        String username = "qa02_" + role.toLowerCase(Locale.ROOT) + "_" + userId;
        jdbc.update("""
                insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                values(?,?,?,?,?,'ACTIVE')
                """, userId, ORG_ID, username, passwords.hash(PASSWORD), "QA-02 " + role);
        assertThat(jdbc.update("""
                insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                select organization_id,?,id,?,now(),? from iam.roles
                where organization_id=? and code=?
                """, userId, STORE_ID, userId, ORG_ID, role)).isEqualTo(1);
        ResponseEntity<Map> login = http.postForEntity("/api/v1/auth/login", Map.of(
                "organizationCode", "SIMTIM", "storeCode", "MAIN",
                "username", username, "password", PASSWORD), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return login.getBody().get("accessToken").toString();
    }

    protected ResponseEntity<Object> post(String path, Object request, String token) {
        return http.exchange(path, HttpMethod.POST,
                new HttpEntity<>(request, headers(token)), Object.class);
    }

    protected ResponseEntity<Object> get(String path, String token) {
        return http.exchange(path, HttpMethod.GET,
                new HttpEntity<>(headers(token)), Object.class);
    }

    protected HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Organization-Id", ORG_ID.toString());
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> body(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(Map.class);
        return (Map<String, Object>) response.getBody();
    }

    protected BigDecimal productBalance(UUID productId) {
        return jdbc.queryForObject("""
                select coalesce(sum(on_hand_quantity),0)
                from inventory.inventory_balances
                where organization_id=? and store_id=? and product_id=?
                """, BigDecimal.class, ORG_ID, STORE_ID, productId);
    }

    protected int count(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }

    protected BigDecimal decimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
