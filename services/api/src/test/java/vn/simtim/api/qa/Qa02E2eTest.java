package vn.simtim.api.qa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * QA-02 independent cross-module gate.
 *
 * <p>The HTTP responses drive the workflow, while PostgreSQL queries provide the oracle for
 * transactionality, ledger traceability and report reconciliation. The database is disposable and
 * contains no operational data.</p>
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Qa02E2eTest {

    private static final UUID ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID SUPPLIER_ID = UUID.fromString("10000000-0000-0000-0000-000000000061");
    private static final UUID RICE_ID = UUID.fromString("10000000-0000-0000-0000-000000000041");
    private static final UUID APPLE_ID = UUID.fromString("10000000-0000-0000-0000-000000000042");
    private static final String PASSWORD = "qa02-disposable-password";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine").withDatabaseName("simtim_qa02_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    private String salesToken;
    private String stockToken;
    private String managerToken;
    private String adminToken;

    @BeforeAll
    void createActors() {
        salesToken = createActor("SALES");
        stockToken = createActor("STOCK");
        managerToken = createActor("MANAGER");
        adminToken = createActor("ADMIN");
    }

    @BeforeEach
    void resetBusinessData() {
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
    @SuppressWarnings("unchecked")
    void receiptToCashInvoiceAndReportsReconcileWithIndependentSqlOracle() {
        UUID appleReceiptId = receive(APPLE_ID, "2.500", "2.500", "0.000", null);
        UUID riceReceiptId = receive(RICE_ID, "1.000", "1.000", "0.000", null);

        List<Map<String, Object>> saleItems = List.of(
                Map.of("productId", APPLE_ID, "quantity", "1.250"),
                Map.of("productId", RICE_ID, "quantity", "1.000"));

        Map<String, Object> quote = body(post("/api/v1/sales/quote", Map.of(
                "items", saleItems), salesToken));
        assertThat(quote.get("grandTotal")).isEqualTo(75000);

        ResponseEntity<Object> checkout = post("/api/v1/sales/checkout", Map.of(
                "items", saleItems,
                "cashAmount", 80000), salesToken);
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> invoice = body(checkout);
        UUID invoiceId = UUID.fromString(invoice.get("id").toString());
        assertThat(invoice).containsEntry("grandTotal", 75000).containsEntry("changeAmount", 5000);

        Map<String, Object> storedInvoice = jdbc.queryForMap("""
                select status, subtotal, discount_total, grand_total, paid_total, change_amount
                from sales.invoices where id=?
                """, invoiceId);
        assertThat(storedInvoice)
                .containsEntry("status", "COMPLETED")
                .containsEntry("subtotal", 75000L)
                .containsEntry("discount_total", 0L)
                .containsEntry("grand_total", 75000L)
                .containsEntry("paid_total", 75000L)
                .containsEntry("change_amount", 5000L);
        assertThat(count("select count(*) from sales.payments where invoice_id=?", invoiceId)).isEqualTo(1);
        assertThat(count("select count(*) from sales.invoice_lines where invoice_id=?", invoiceId)).isEqualTo(2);
        assertThat(count("select count(*) from sales.invoice_line_batches ilb join sales.invoice_lines il on il.id=ilb.invoice_line_id where il.invoice_id=?", invoiceId)).isEqualTo(2);

        List<Map<String, Object>> batches = jdbc.queryForList("""
                select ib.batch_id, ib.on_hand_quantity,
                       coalesce(sum(sm.quantity_delta), 0) as ledger_quantity
                from inventory.inventory_balances ib
                left join inventory.stock_movements sm
                  on sm.organization_id=ib.organization_id and sm.store_id=ib.store_id
                 and sm.product_id=ib.product_id and sm.batch_id=ib.batch_id
                where ib.organization_id=? and ib.store_id=? and ib.product_id=?
                group by ib.batch_id, ib.on_hand_quantity
                """, ORG_ID, STORE_ID, APPLE_ID);
        assertThat(batches).hasSize(1);
        assertThat(decimal(batches.get(0).get("on_hand_quantity"))).isEqualByComparingTo("1.250");
        assertThat(decimal(batches.get(0).get("ledger_quantity"))).isEqualByComparingTo("1.250");
        assertThat(count("select count(*) from inventory.stock_movements where reference_id=? and reference_type='GOODS_RECEIPT'", appleReceiptId)).isEqualTo(1);
        assertThat(count("select count(*) from inventory.stock_movements where reference_id=? and reference_type='GOODS_RECEIPT'", riceReceiptId)).isEqualTo(1);
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'", new Object[0])).isEqualTo(2);

        LocalDate today = LocalDate.now();
        Map<String, Object> revenue = body(get("/api/v1/reports/revenue?from=" + today + "&to=" + today.plusDays(1), managerToken));
        Long sqlRevenue = jdbc.queryForObject("""
                select coalesce(sum(grand_total),0) from sales.invoices
                where organization_id=? and store_id=? and status='COMPLETED'
                """, Long.class, ORG_ID, STORE_ID);
        Integer sqlInvoiceCount = jdbc.queryForObject("""
                select count(*) from sales.invoices
                where organization_id=? and store_id=? and status='COMPLETED'
                """, Integer.class, ORG_ID, STORE_ID);
        assertThat(decimal(revenue.get("revenue"))).isEqualByComparingTo(sqlRevenue.toString());
        assertThat(decimal(revenue.get("invoiceCount"))).isEqualByComparingTo(sqlInvoiceCount.toString());

        Map<String, Object> inventoryReport = body(get("/api/v1/reports/inventory", managerToken));
        List<Map<String, Object>> items = (List<Map<String, Object>>) inventoryReport.get("items");
        Map<String, Object> apple = items.stream()
                .filter(item -> APPLE_ID.toString().equals(item.get("productId")))
                .findFirst().orElseThrow();
        assertThat(decimal(apple.get("quantity"))).isEqualByComparingTo("1.250");
        assertThat(apple.get("status")).isEqualTo("VALID");
    }

    @Test
    void insufficientSecondProductRollsBackTheWholeCart() {
        receive(RICE_ID, "1.000", "1.000", "0.000", null);
        receive(APPLE_ID, "1.000", "1.000", "0.000", null);
        BigDecimal riceBefore = productBalance(RICE_ID);
        BigDecimal appleBefore = productBalance(APPLE_ID);

        ResponseEntity<Object> response = post("/api/v1/sales/checkout", Map.of(
                "items", List.of(
                        Map.of("productId", RICE_ID, "quantity", "1.000"),
                        Map.of("productId", APPLE_ID, "quantity", "2.000")),
                "cashAmount", 200000), salesToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(response)).containsEntry("code", "CONFLICT");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo(riceBefore);
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo(appleBefore);
        assertThat(count("select count(*) from sales.invoices", new Object[0])).isZero();
        assertThat(count("select count(*) from sales.payments", new Object[0])).isZero();
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'", new Object[0])).isZero();
    }

    @Test
    void concurrentCheckoutsSerializeAndOnlyOneCanConsumeTheLastUnit() throws Exception {
        receive(APPLE_ID, "1.000", "1.000", "0.000", null);
        Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", APPLE_ID, "quantity", "1.000")),
                "cashAmount", 40000);

        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return post("/api/v1/sales/checkout", request, salesToken);
            });
            var second = executor.submit(() -> {
                start.await();
                return post("/api/v1/sales/checkout", request, salesToken);
            });
            start.countDown();
            List<Integer> statuses = List.of(
                    first.get(15, TimeUnit.SECONDS).getStatusCode().value(),
                    second.get(15, TimeUnit.SECONDS).getStatusCode().value());
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        }

        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("0.000");
        assertThat(count("select count(*) from sales.invoices", new Object[0])).isEqualTo(1);
        assertThat(count("select count(*) from sales.payments", new Object[0])).isEqualTo(1);
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'", new Object[0])).isEqualTo(1);
    }

    @Test
    void reportPermissionsAndSalesInventoryProjectionAreEnforcedByRealSecurityChain() {
        assertThat(get("/api/v1/auth/session", managerToken).getStatusCode().value())
                .as("manager session must remain valid for the whole isolated suite")
                .isEqualTo(200);
        assertThat(get("/api/v1/reports/inventory", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/reports/inventory", salesToken).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/v1/reports/inventory", stockToken).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/v1/reports/inventory", managerToken).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/reports/inventory", adminToken).getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Object> inventory = get("/api/v1/inventory/products", salesToken);
        assertThat(inventory.getStatusCode().value()).isEqualTo(200);
        assertThat(String.valueOf(inventory.getBody()))
                .doesNotContain("unitCost")
                .doesNotContain("unit_cost")
                .doesNotContain("purchase");
        assertThat(get("/api/v1/inventory/receipts", salesToken).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void fullyRejectedReceiptPersistsTheDiscrepancyButReplayCreatesNoStockTwice() {
        UUID operationId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "supplierId", SUPPLIER_ID,
                "clientOperationId", operationId,
                "lines", List.of(Map.of(
                        "productId", APPLE_ID,
                        "expectedQuantity", "2.000",
                        "deliveredQuantity", "2.000",
                        "acceptedQuantity", "0.000",
                        "rejectedQuantity", "2.000",
                        "unitCost", 10000,
                        "expiryDate", LocalDate.now().plusDays(8).toString(),
                        "discrepancyReason", "Hàng dập hỏng")));
        HttpHeaders requestHeaders = headers(stockToken);
        requestHeaders.set("Idempotency-Key", idempotencyKey.toString());
        HttpEntity<Object> entity = new HttpEntity<>(request, requestHeaders);

        ResponseEntity<Object> first = http.exchange(
                "/api/v1/inventory/receipts", HttpMethod.POST, entity, Object.class);
        ResponseEntity<Object> replay = http.exchange(
                "/api/v1/inventory/receipts", HttpMethod.POST, entity, Object.class);

        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getStatusCode().value()).isIn(200, 201);
        UUID receiptId = UUID.fromString(body(first).get("id").toString());
        assertThat(body(replay).get("id")).isEqualTo(receiptId.toString());
        assertThat(count("select count(*) from inventory.goods_receipts where client_operation_id=?", operationId)).isEqualTo(1);
        assertThat(count("select count(*) from inventory.goods_receipt_lines where receipt_id=? and accepted_quantity=0 and rejected_quantity=2", receiptId)).isEqualTo(1);
        assertThat(count("select count(*) from inventory.product_batches", new Object[0])).isZero();
        assertThat(count("select count(*) from inventory.inventory_balances", new Object[0])).isZero();
        assertThat(count("select count(*) from inventory.stock_movements", new Object[0])).isZero();
    }

    private UUID receive(UUID productId, String delivered, String accepted, String rejected, String reason) {
        UUID clientOperationId = UUID.randomUUID();
        var line = new java.util.LinkedHashMap<String, Object>();
        line.put("productId", productId);
        line.put("expectedQuantity", delivered);
        line.put("deliveredQuantity", delivered);
        line.put("acceptedQuantity", accepted);
        line.put("rejectedQuantity", rejected);
        line.put("unitCost", 10000);
        line.put("expiryDate", LocalDate.now().plusDays(8).toString());
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

    private String createActor(String role) {
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

    private ResponseEntity<Object> post(String path, Object request, String token) {
        return http.exchange(path, HttpMethod.POST,
                new HttpEntity<>(request, headers(token)), Object.class);
    }

    private ResponseEntity<Object> get(String path, String token) {
        return http.exchange(path, HttpMethod.GET,
                new HttpEntity<>(headers(token)), Object.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Organization-Id", ORG_ID.toString());
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(Map.class);
        return (Map<String, Object>) response.getBody();
    }

    private BigDecimal productBalance(UUID productId) {
        return jdbc.queryForObject("""
                select coalesce(sum(on_hand_quantity),0)
                from inventory.inventory_balances
                where organization_id=? and store_id=? and product_id=?
                """, BigDecimal.class, ORG_ID, STORE_ID, productId);
    }

    private int count(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }

    private BigDecimal decimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
