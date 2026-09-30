package vn.simtim.api.sale;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.simtim.api.auth.domain.PasswordHasher;

/**
 * Integration test cho SAL-01: Quote, Checkout CASH, hóa đơn và trừ tồn.
 * Dùng Testcontainers PostgreSQL 17 và profile demo.
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SaleApiTest {

    static final String ORG_ID    = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID  = "10000000-0000-0000-0000-000000000002";
    static final String RICE_ID   = "10000000-0000-0000-0000-000000000041";  // ST-RICE-01
    static final String APPLE_ID  = "10000000-0000-0000-0000-000000000042";  // ST-APPLE-01

    // Demo seed batch IDs
    static final String RICE_BATCH_ID  = "10000000-0000-0000-0000-0000000000a1";
    static final String APPLE_BATCH_ID = "10000000-0000-0000-0000-0000000000a2";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_sale_test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        String ext = System.getenv("SIMTIM_TEST_DB_URL");
        if (ext == null || ext.isBlank()) {
            postgres.start();
            r.add("spring.datasource.url", postgres::getJdbcUrl);
            r.add("spring.datasource.username", postgres::getUsername);
            r.add("spring.datasource.password", postgres::getPassword);
        } else {
            r.add("spring.datasource.url", () -> ext);
            r.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            r.add("spring.datasource.password", () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    private String salesToken;
    private String stockToken;
    private String managerToken;

    @BeforeAll
    void setup() {
        salesToken = createUserAndLogin("sales-sale-test", "SALES");
        stockToken = createUserAndLogin("stock-sale-test", "STOCK");
        managerToken = createUserAndLogin("mgr-sale-test", "MANAGER");

        // Seed demo inventory if not already present (DemoAccounts only runs when SIMTIM_DEMO_PASSWORD is set)
        UUID actorId = fixtureUsers.get(2); // MANAGER user
        seedInventoryIfAbsent(actorId);

        // Reset demo batches to known quantities before each test run
        jdbc.update("UPDATE inventory.inventory_balances SET quantity_on_hand = 100 WHERE product_batch_id = ?::uuid",
                RICE_BATCH_ID);
        jdbc.update("UPDATE inventory.inventory_balances SET quantity_on_hand = 50  WHERE product_batch_id = ?::uuid",
                APPLE_BATCH_ID);
        // Clean up invoices from any previous partial run
        jdbc.update("DELETE FROM inventory.stock_movements WHERE actor_user_id IN " +
                "(SELECT id FROM iam.users WHERE username LIKE '%-sale-test')");
        jdbc.update("DELETE FROM sales.payments WHERE organization_id = ?::uuid", ORG_ID);
        jdbc.update("DELETE FROM sales.invoice_line_batches WHERE organization_id = ?::uuid", ORG_ID);
        jdbc.update("DELETE FROM sales.invoice_lines WHERE organization_id = ?::uuid", ORG_ID);
        jdbc.update("DELETE FROM sales.invoices WHERE organization_id = ?::uuid", ORG_ID);
    }

    /** Insert demo goods_receipt chain if inventory_balances rows don't yet exist. */
    private void seedInventoryIfAbsent(UUID actorId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM inventory.inventory_balances WHERE product_batch_id = ?::uuid",
                Integer.class, RICE_BATCH_ID);
        if (count != null && count > 0) return; // already seeded

        UUID supplierId   = UUID.fromString("10000000-0000-0000-0000-000000000061");
        UUID productRice  = UUID.fromString(RICE_ID);
        UUID productApple = UUID.fromString(APPLE_ID);
        UUID receiptId    = UUID.fromString("10000000-0000-0000-0000-000000000081");
        UUID lineRiceId   = UUID.fromString("10000000-0000-0000-0000-000000000091");
        UUID lineAppleId  = UUID.fromString("10000000-0000-0000-0000-000000000092");
        UUID batchRiceId  = UUID.fromString(RICE_BATCH_ID);
        UUID batchAppleId = UUID.fromString(APPLE_BATCH_ID);
        UUID storeId      = UUID.fromString(STORE_ID);
        UUID orgId        = UUID.fromString(ORG_ID);

        jdbc.update("INSERT INTO inventory.goods_receipts " +
                "(id,organization_id,store_id,supplier_id,receipt_no,status,received_at,created_by,confirmed_by) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,'RCV-DEMO-001','CONFIRMED','2026-01-01T00:00:00Z',?,?) " +
                "ON CONFLICT DO NOTHING",
                receiptId, orgId, storeId, supplierId, actorId, actorId);

        jdbc.update("INSERT INTO inventory.goods_receipt_lines " +
                "(id,organization_id,store_id,receipt_id,product_id," +
                " expected_quantity,delivered_quantity,accepted_quantity,rejected_quantity,unit_cost) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,?::uuid,100,100,100,0,18000) ON CONFLICT DO NOTHING",
                lineRiceId, orgId, storeId, receiptId, productRice);

        jdbc.update("INSERT INTO inventory.goods_receipt_lines " +
                "(id,organization_id,store_id,receipt_id,product_id," +
                " expected_quantity,delivered_quantity,accepted_quantity,rejected_quantity,unit_cost,expiry_date) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,?::uuid,50,50,50,0,28000,'2026-12-31') ON CONFLICT DO NOTHING",
                lineAppleId, orgId, storeId, receiptId, productApple);

        jdbc.update("INSERT INTO inventory.product_batches " +
                "(id,organization_id,store_id,product_id,receipt_line_id," +
                " internal_batch_code,received_date,expiry_date,status) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,?::uuid,'BATCH-RICE-001','2026-01-01',null,'AVAILABLE') " +
                "ON CONFLICT DO NOTHING",
                batchRiceId, orgId, storeId, productRice, lineRiceId);

        jdbc.update("INSERT INTO inventory.product_batches " +
                "(id,organization_id,store_id,product_id,receipt_line_id," +
                " internal_batch_code,received_date,expiry_date,status) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,?::uuid,'BATCH-APPLE-001','2026-01-01','2026-12-31','AVAILABLE') " +
                "ON CONFLICT DO NOTHING",
                batchAppleId, orgId, storeId, productApple, lineAppleId);

        jdbc.update("INSERT INTO inventory.inventory_balances " +
                "(id,organization_id,store_id,product_batch_id,quantity_on_hand) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,100) ON CONFLICT DO NOTHING",
                UUID.fromString("10000000-0000-0000-0000-0000000000b1"), orgId, storeId, batchRiceId);

        jdbc.update("INSERT INTO inventory.inventory_balances " +
                "(id,organization_id,store_id,product_batch_id,quantity_on_hand) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,50) ON CONFLICT DO NOTHING",
                UUID.fromString("10000000-0000-0000-0000-0000000000b2"), orgId, storeId, batchAppleId);

        jdbc.update("INSERT INTO inventory.stock_movements " +
                "(id,organization_id,store_id,product_batch_id,movement_type," +
                " quantity_delta,receipt_line_id,occurred_at,actor_user_id) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,'RECEIPT',100,?::uuid,'2026-01-01T00:00:00Z',?) " +
                "ON CONFLICT DO NOTHING",
                UUID.fromString("10000000-0000-0000-0000-0000000000c1"), orgId, storeId, batchRiceId, lineRiceId, actorId);

        jdbc.update("INSERT INTO inventory.stock_movements " +
                "(id,organization_id,store_id,product_batch_id,movement_type," +
                " quantity_delta,receipt_line_id,occurred_at,actor_user_id) " +
                "VALUES(?::uuid,?::uuid,?::uuid,?::uuid,'RECEIPT',50,?::uuid,'2026-01-01T00:00:00Z',?) " +
                "ON CONFLICT DO NOTHING",
                UUID.fromString("10000000-0000-0000-0000-0000000000c2"), orgId, storeId, batchAppleId, lineAppleId, actorId);
    }


    @AfterAll
    void teardown() {
        try {
            // Delete inventory seed data in FK-safe order (goods_receipts.created_by → fixture MANAGER user)
            jdbc.update("DELETE FROM inventory.stock_movements WHERE id IN " +
                    "('10000000-0000-0000-0000-0000000000c1'::uuid,'10000000-0000-0000-0000-0000000000c2'::uuid)");
            jdbc.update("DELETE FROM inventory.stock_movements WHERE actor_user_id IN " +
                    "(SELECT id FROM iam.users WHERE username LIKE '%-sale-test')");
            jdbc.update("DELETE FROM sales.payments WHERE organization_id = ?::uuid", ORG_ID);
            jdbc.update("DELETE FROM sales.invoice_line_batches WHERE organization_id = ?::uuid", ORG_ID);
            jdbc.update("DELETE FROM sales.invoice_lines WHERE organization_id = ?::uuid", ORG_ID);
            jdbc.update("DELETE FROM sales.invoices WHERE organization_id = ?::uuid", ORG_ID);
            jdbc.update("DELETE FROM inventory.inventory_balances WHERE id IN " +
                    "('10000000-0000-0000-0000-0000000000b1'::uuid,'10000000-0000-0000-0000-0000000000b2'::uuid)");
            jdbc.update("DELETE FROM inventory.product_batches WHERE id IN " +
                    "('10000000-0000-0000-0000-0000000000a1'::uuid,'10000000-0000-0000-0000-0000000000a2'::uuid)");
            jdbc.update("DELETE FROM inventory.goods_receipt_lines WHERE id IN " +
                    "('10000000-0000-0000-0000-000000000091'::uuid,'10000000-0000-0000-0000-000000000092'::uuid)");
            jdbc.update("DELETE FROM inventory.goods_receipts WHERE id = '10000000-0000-0000-0000-000000000081'::uuid");
            for (UUID id : fixtureUsers) {
                jdbc.update("DELETE FROM iam.auth_sessions WHERE user_id = ?", id);
                jdbc.update("DELETE FROM iam.user_roles WHERE user_id = ?", id);
                jdbc.update("DELETE FROM iam.users WHERE id = ?", id);
            }
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    // =========================================================================
    // Quote tests
    // =========================================================================

    @Test
    @Order(1)
    void quote_returnsCorrectPriceAndTotal() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 2)));

        var resp = post("/api/v1/invoices/quote", body, salesToken);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var r = (Map<String, Object>) resp.getBody();
        assertThat(r).isNotNull();
        assertThat(r.get("grandTotal")).isEqualTo(50000); // 2 × 25000
        @SuppressWarnings("unchecked")
        var lines = (List<Map<String, Object>>) r.get("lines");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).get("unitPrice")).isEqualTo(25000);
    }

    @Test
    @Order(2)
    void quote_unknownProduct_returns422() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", 1)));
        var resp = post("/api/v1/invoices/quote", body, salesToken);
        assertThat(resp.getStatusCode().value()).isIn(422, 404);
    }

    @Test
    @Order(3)
    void quote_emptyItems_returns4xx() {
        var body = Map.of("storeId", STORE_ID, "items", List.of());
        var resp = post("/api/v1/invoices/quote", body, salesToken);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    // =========================================================================
    // Checkout tests
    // =========================================================================

    @Test
    @Order(10)
    void checkout_cashSale_createsInvoiceAndReducesStock() {
        long riceBefore = getBalance(RICE_BATCH_ID);

        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 3)),
                "cashAmount", 100000L);
        var resp = post("/api/v1/invoices", body, salesToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        var inv = (Map<String, Object>) resp.getBody();
        assertThat(inv).isNotNull();
        assertThat(inv.get("status")).isEqualTo("COMPLETED");
        assertThat(inv.get("grandTotal")).isEqualTo(75000); // 3 × 25000
        assertThat(inv.get("paidTotal")).isEqualTo(100000);
        @SuppressWarnings("unchecked")
        var lines = (List<?>) inv.get("lines");
        assertThat(lines).hasSize(1);
        @SuppressWarnings("unchecked")
        var payments = (List<?>) inv.get("payments");
        assertThat(payments).hasSize(1);

        // Verify stock reduced
        long riceAfter = getBalance(RICE_BATCH_ID);
        assertThat(riceAfter).isEqualTo(riceBefore - 3);

        // Verify stock_movement created
        int movCount = jdbc.queryForObject(
                "SELECT count(*) FROM inventory.stock_movements WHERE movement_type = 'SALE' AND product_batch_id = ?::uuid",
                Integer.class, RICE_BATCH_ID);
        assertThat(movCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(11)
    void checkout_multipleProducts_correctTotalsAndStockReduction() {
        long riceBefore = getBalance(RICE_BATCH_ID);
        long appleBefore = getBalance(APPLE_BATCH_ID);

        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(
                        Map.of("productId", RICE_ID, "quantity", 2),
                        Map.of("productId", APPLE_ID, "quantity", 1)),
                "cashAmount", 120000L);
        var resp = post("/api/v1/invoices", body, salesToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        var inv = (Map<String, Object>) resp.getBody();
        // subtotal = 2×25000 + 1×40000 = 90000
        assertThat(inv.get("grandTotal")).isEqualTo(90000);
        assertThat(getBalance(RICE_BATCH_ID)).isEqualTo(riceBefore - 2);
        assertThat(getBalance(APPLE_BATCH_ID)).isEqualTo(appleBefore - 1);
    }

    @Test
    @Order(12)
    void checkout_insufficientStock_returns409() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 9999)),
                "cashAmount", 999999999L);
        var resp = post("/api/v1/invoices", body, salesToken);
        assertThat(resp.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @Order(13)
    void checkout_cashLessThanTotal_returns422() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 2)),
                "cashAmount", 1L);  // 1 VND << 50000
        var resp = post("/api/v1/invoices", body, salesToken);
        assertThat(resp.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    @Order(14)
    void checkout_stockRoleCannotCreate_returns403() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 1)),
                "cashAmount", 30000L);
        var resp = post("/api/v1/invoices", body, stockToken);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @Order(15)
    void checkout_unauthenticated_returns401() {
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 1)),
                "cashAmount", 30000L);
        var resp = post("/api/v1/invoices", body, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    // =========================================================================
    // Get/List invoice tests
    // =========================================================================

    @Test
    @Order(20)
    void getInvoice_returnsCreatedInvoice() {
        // Create an invoice first
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", RICE_ID, "quantity", 1)),
                "cashAmount", 30000L);
        var createResp = post("/api/v1/invoices", body, salesToken);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        @SuppressWarnings("unchecked")
        String invoiceId = ((Map<String, Object>) createResp.getBody()).get("id").toString();

        // Fetch it
        var getResp = get("/api/v1/invoices/" + invoiceId, salesToken);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var inv = (Map<String, Object>) getResp.getBody();
        assertThat(inv.get("id")).isEqualTo(invoiceId);
        assertThat(inv.get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @Order(21)
    void listInvoices_returnsInvoicesForStore() {
        var resp = rest.exchange(
                "/api/v1/invoices?storeId=" + STORE_ID,
                HttpMethod.GET, headers(salesToken, null), Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @Order(22)
    void getInvoice_notFound_returns404() {
        var resp = get("/api/v1/invoices/" + UUID.randomUUID(), salesToken);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(23)
    void listInvoices_stockRole_returns200() {
        var resp = rest.exchange(
                "/api/v1/invoices?storeId=" + STORE_ID,
                HttpMethod.GET, headers(stockToken, null), Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(24)
    void listInvoices_unauthenticated_returns401() {
        var resp = rest.exchange(
                "/api/v1/invoices?storeId=" + STORE_ID,
                HttpMethod.GET, headers(null, null), Object.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    // =========================================================================
    // FEFO / stock integrity
    // =========================================================================

    @Test
    @Order(30)
    void checkout_fefoOrder_selectsBatchWithEarliestExpiry() {
        // Apple (ST-APPLE-01) tracks expiry — verify it uses the demo batch (2026-12-31)
        long appleBefore = getBalance(APPLE_BATCH_ID);
        var body = Map.of(
                "storeId", STORE_ID,
                "items", List.of(Map.of("productId", APPLE_ID, "quantity", 5)),
                "cashAmount", 300000L);
        var resp = post("/api/v1/invoices", body, managerToken);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(getBalance(APPLE_BATCH_ID)).isEqualTo(appleBefore - 5);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String createUserAndLogin(String username, String roleCode) {
        UUID userId = UUID.randomUUID();
        fixtureUsers.add(userId);
        String hash = passwords.hash("test-sal01-pass");
        jdbc.update("INSERT INTO iam.users(id,organization_id,username,password_hash,full_name,status) " +
                "VALUES(?,?::uuid,?,?,'SAL-01 Test','ACTIVE')",
                userId, ORG_ID, username, hash);
        jdbc.update("INSERT INTO iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by) " +
                "SELECT ?::uuid,?,r.id,?::uuid,now(),? FROM iam.roles r " +
                "WHERE r.organization_id=?::uuid AND r.code=?",
                ORG_ID, userId, STORE_ID, userId, ORG_ID, roleCode);
        return login(username);
    }

    private String login(String username) {
        var resp = rest.postForEntity("/api/v1/auth/login",
                Map.of("username", username, "password", "test-sal01-pass",
                        "organizationCode", "SIMTIM", "storeCode", "MAIN"),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) resp.getBody();
        return (String) body.get("accessToken");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Object> post(String path, Object body, String token) {
        return rest.exchange(path, HttpMethod.POST, headers(token, body), Object.class);
    }

    @SuppressWarnings({"unchecked"})
    private ResponseEntity<Object> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, headers(token, null), Object.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForOk(String path, Object body, String token) {
        var resp = post(path, body, token);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        return (Map<String, Object>) resp.getBody();
    }

    private HttpEntity<?> headers(String token, Object body) {
        var h = new HttpHeaders();
        h.set("X-Organization-Id", ORG_ID);
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.set("Authorization", "Bearer " + token);
        return body != null ? new HttpEntity<>(body, h) : new HttpEntity<>(h);
    }

    private long getBalance(String batchId) {
        BigDecimal qty = jdbc.queryForObject(
                "SELECT quantity_on_hand FROM inventory.inventory_balances WHERE product_batch_id = ?::uuid",
                BigDecimal.class, batchId);
        return qty == null ? 0L : qty.longValue();
    }
}
