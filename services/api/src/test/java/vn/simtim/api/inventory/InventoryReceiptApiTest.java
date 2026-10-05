package vn.simtim.api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
 * Provider integration test cho INV-01: Transaction nhận hàng.
 * Kiểm tra: xác nhận phiếu, idempotency, quyền và dữ liệu nguyên tử.
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryReceiptApiTest {

    static final String ORG_ID   = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID = "10000000-0000-0000-0000-000000000002";
    static final String SUPPLIER_ID = "10000000-0000-0000-0000-000000000061";
    static final String PRODUCT_RICE_ID = "10000000-0000-0000-0000-000000000041";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_inv_test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        String externalUrl = System.getenv("SIMTIM_TEST_DB_URL");
        if (externalUrl == null || externalUrl.isBlank()) {
            postgres.start();
            r.add("spring.datasource.url",      postgres::getJdbcUrl);
            r.add("spring.datasource.username", postgres::getUsername);
            r.add("spring.datasource.password", postgres::getPassword);
        } else {
            r.add("spring.datasource.url",      () -> externalUrl);
            r.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            r.add("spring.datasource.password", () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @AfterAll
    void stop() {
        try {
            UUID orgId = UUID.fromString(ORG_ID);
            jdbc.update("delete from inventory.stock_movements where organization_id=?", orgId);
            jdbc.update("delete from inventory.inventory_balances where organization_id=?", orgId);
            jdbc.update("delete from inventory.product_batches where organization_id=?", orgId);
            jdbc.update("delete from inventory.goods_receipt_lines where organization_id=?", orgId);
            jdbc.update("delete from inventory.goods_receipts where organization_id=?", orgId);
            for (UUID id : fixtureUsers) {
                jdbc.update("delete from iam.auth_sessions where user_id=?", id);
                jdbc.update("delete from iam.user_roles where user_id=?", id);
                jdbc.update("delete from iam.users where id=?", id);
            }
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    private static final String PASSWORD = "inv-fixture-password-123";

    private String stockToken;
    private String salesToken;

    @BeforeAll
    void setup() {
        String hash = passwords.hash(PASSWORD);
        stockToken = createSession("STOCK", hash);
        salesToken = createSession("SALES", hash);
    }

    private String createSession(String role, String hash) {
        UUID id = UUID.randomUUID();
        String username = "inv_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                values(?,?,?,?,?,'ACTIVE')
                """, id, UUID.fromString(ORG_ID), username, hash, "Inv fixture " + role);
        fixtureUsers.add(id);
        assertThat(jdbc.update("""
                insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                select organization_id,?,id,?,now(),? from iam.roles
                where organization_id=? and code=?
                """, id, UUID.fromString(STORE_ID), id, UUID.fromString(ORG_ID), role)).isEqualTo(1);
        var resp = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of(
                "organizationCode", "SIMTIM", "storeCode", "MAIN",
                "username", username, "password", PASSWORD), jsonHeaders()), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        return (String) resp.getBody().get("accessToken");
    }

    // ── POST /api/v1/inventory/receipts ──────────────────────────────────────

    @Test
    void confirmReceipt_stock_creates201WithBody() {
        UUID idemKey = UUID.randomUUID();
        UUID clientOpId = UUID.randomUUID();

        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(validReceiptBody(clientOpId), authHeaders(stockToken, idemKey)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody()).containsKey("id");
        assertThat(resp.getBody()).containsEntry("status", "CONFIRMED");
        assertThat(resp.getBody()).containsKey("lines");

        // Verify location header
        assertThat(resp.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void confirmReceipt_idempotency_sameKeyReturns201Again() {
        UUID idemKey    = UUID.randomUUID();
        UUID clientOpId = UUID.randomUUID();
        var body = validReceiptBody(clientOpId);
        var headers = authHeaders(stockToken, idemKey);

        var first  = rest.exchange("/api/v1/inventory/receipts", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
        var second = rest.exchange("/api/v1/inventory/receipts", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(first.getStatusCode().value()).isEqualTo(201);
        // Second call with same key+payload → same document (idempotent)
        assertThat(second.getStatusCode().value()).isIn(200, 201);
        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    @Test
    void confirmReceipt_withoutBearer_returns401() {
        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(validReceiptBody(UUID.randomUUID()), jsonHeaders()),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).containsEntry("code", "UNAUTHENTICATED");
    }

    @Test
    void confirmReceipt_salesRole_returns403() {
        UUID idemKey = UUID.randomUUID();
        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(validReceiptBody(UUID.randomUUID()), authHeaders(salesToken, idemKey)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
        assertThat(resp.getBody()).containsEntry("code", "FORBIDDEN");
    }

    @Test
    void confirmReceipt_withDiscrepancy_requiresReason() {
        UUID idemKey = UUID.randomUUID();
        // accepted + rejected = delivered, but delivered != expected → need discrepancyReason
        var body = Map.of(
                "supplierId", SUPPLIER_ID,
                "clientOperationId", UUID.randomUUID().toString(),
                "lines", List.of(Map.of(
                        "productId", PRODUCT_RICE_ID,
                        "expectedQuantity", "10.000",
                        "deliveredQuantity", "8.000",
                        "acceptedQuantity", "8.000",
                        "rejectedQuantity", "0.000",
                        "unitCost", 5000
                        // missing discrepancyReason
                )));
        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(stockToken, idemKey)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(422);
        assertThat(resp.getBody()).containsEntry("code", "INVALID_RECEIPT");
    }

    // ── GET /api/v1/inventory/receipts ───────────────────────────────────────

    @Test
    void listReceipts_stock_returns200() {
        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(stockToken)),
                Object[].class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void listReceipts_clientOperationId_recoversCommittedPostAfterResponseLoss() {
        UUID clientOpId = UUID.randomUUID();

        // Simulate the client losing the POST response: commit it, then deliberately
        // recover only through the public GET filter without using the response body.
        var post = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(validReceiptBody(clientOpId),
                        authHeaders(stockToken, UUID.randomUUID())),
                Map.class);
        assertThat(post.getStatusCode().value()).isEqualTo(201);

        var recovery = rest.exchange(
                "/api/v1/inventory/receipts?clientOperationId=" + clientOpId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(stockToken)),
                Map[].class);

        assertThat(recovery.getStatusCode().value()).isEqualTo(200);
        assertThat(recovery.getBody()).hasSize(1);
        assertThat(recovery.getBody()[0]).containsEntry("clientOperationId", clientOpId.toString());
    }

    @Test
    void listReceipts_clientOperationId_doesNotLeakAnotherStore() {
        UUID otherStoreId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        UUID clientOpId = UUID.randomUUID();
        UUID organizationId = UUID.fromString(ORG_ID);
        jdbc.update("""
                insert into core.stores(id,organization_id,code,name,status)
                values(?,?,?,?,'ACTIVE')
                """, otherStoreId, organizationId,
                "OTHER-" + otherStoreId.toString().substring(0, 8),
                "Other inventory fixture");
        try {
            jdbc.update("""
                    insert into inventory.goods_receipts
                        (id,organization_id,store_id,supplier_id,status,received_at,confirmed_by,
                         client_operation_id,idempotency_key,payload_hash)
                    values(?,?,?,?,'CONFIRMED',now(),?,?,?,'\\x00'::bytea)
                    """, receiptId, organizationId, otherStoreId, UUID.fromString(SUPPLIER_ID),
                    fixtureUsers.get(0), clientOpId, UUID.randomUUID());

            var recovery = rest.exchange(
                    "/api/v1/inventory/receipts?clientOperationId=" + clientOpId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(stockToken)),
                    Map[].class);

            assertThat(recovery.getStatusCode().value()).isEqualTo(200);
            assertThat(recovery.getBody()).isEmpty();
        } finally {
            jdbc.update("delete from inventory.goods_receipts where id=?", receiptId);
            jdbc.update("delete from core.stores where id=?", otherStoreId);
        }
    }

    /**
     * Chứng minh filter clientOperationId xảy ra TRƯỚC phân trang (filter-before-pagination).
     * <p>Kịch bản: tạo 3 phiếu, sau đó lấy danh sách với limit=1 và đặt clientOperationId
     * của phiếu cuối cùng. Kết quả phải trả đúng 1 phiếu — phiếu khớp filter —
     * bất kể limit/offset; nếu pagination được áp trước filter thì phiếu đó sẽ bị cắt mất.</p>
     */
    @Test
    void listReceipts_clientOperationId_filterBeforePagination() {
        // Tạo 3 phiếu — clientOpIdTarget là phiếu thứ hai (giữa)
        UUID clientOpIdOther1 = UUID.randomUUID();
        UUID clientOpIdTarget = UUID.randomUUID();
        UUID clientOpIdOther2 = UUID.randomUUID();

        for (UUID opId : new UUID[]{clientOpIdOther1, clientOpIdTarget, clientOpIdOther2}) {
            var post = rest.exchange("/api/v1/inventory/receipts",
                    HttpMethod.POST,
                    new HttpEntity<>(validReceiptBody(opId),
                            authHeaders(stockToken, UUID.randomUUID())),
                    Map.class);
            assertThat(post.getStatusCode().value()).isEqualTo(201);
        }

        // Lấy danh sách với limit=1 và filter clientOperationId — filter phải thắng pagination
        var resp = rest.exchange(
                "/api/v1/inventory/receipts?clientOperationId=" + clientOpIdTarget + "&limit=1&offset=0",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(stockToken)),
                Map[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        // Phải trả đúng 1 phần tử và đúng clientOperationId — không bị cắt bởi limit
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody()[0]).containsEntry("clientOperationId", clientOpIdTarget.toString());
    }

    /**
     * Chứng minh filter clientOperationId chỉ trả phiếu đúng clientOperationId,
     * không trả phiếu khác của cùng store.
     */
    @Test
    void listReceipts_clientOperationId_exactMatchOnly() {
        UUID clientOpIdA = UUID.randomUUID();
        UUID clientOpIdB = UUID.randomUUID();

        // Tạo phiếu A và phiếu B trong cùng store
        for (UUID opId : new UUID[]{clientOpIdA, clientOpIdB}) {
            var post = rest.exchange("/api/v1/inventory/receipts",
                    HttpMethod.POST,
                    new HttpEntity<>(validReceiptBody(opId),
                            authHeaders(stockToken, UUID.randomUUID())),
                    Map.class);
            assertThat(post.getStatusCode().value()).isEqualTo(201);
        }

        // Filter theo A — phải chỉ trả A, không trả B
        var resp = rest.exchange(
                "/api/v1/inventory/receipts?clientOperationId=" + clientOpIdA,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(stockToken)),
                Map[].class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody()[0]).containsEntry("clientOperationId", clientOpIdA.toString());
        // Đảm bảo không lẫn phiếu B
        for (var body : resp.getBody()) {
            assertThat(body).doesNotContainEntry("clientOperationId", clientOpIdB.toString());
        }
    }

    @Test
    void listReceipts_sales_returns403() {
        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(salesToken)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getReceipt_unknownId_returns404() {
        var resp = rest.exchange("/api/v1/inventory/receipts/" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(stockToken)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).containsEntry("code", "NOT_FOUND");
    }

    // ── Atomic: balance and movement created ────────────────────────────────

    @Test
    void confirmReceipt_createsBalanceAndMovement() {
        UUID idemKey    = UUID.randomUUID();
        UUID clientOpId = UUID.randomUUID();

        var resp = rest.exchange("/api/v1/inventory/receipts",
                HttpMethod.POST,
                new HttpEntity<>(validReceiptBody(clientOpId), authHeaders(stockToken, idemKey)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);

        String receiptId = (String) resp.getBody().get("id");

        // Balance must exist
        Integer balCount = jdbc.queryForObject(
                "SELECT count(*) FROM inventory.inventory_balances ib " +
                "JOIN inventory.product_batches pb ON pb.id = ib.batch_id " +
                "JOIN inventory.goods_receipt_lines grl ON grl.id = pb.receipt_line_id " +
                "WHERE grl.receipt_id = ?",
                Integer.class, UUID.fromString(receiptId));
        assertThat(balCount).isGreaterThan(0);

        // Stock movement RECEIPT must exist
        Integer mvCount = jdbc.queryForObject(
                "SELECT count(*) FROM inventory.stock_movements " +
                "WHERE reference_id=? AND reference_type='GOODS_RECEIPT' AND movement_type='RECEIPT'",
                Integer.class, UUID.fromString(receiptId));
        assertThat(mvCount).isGreaterThan(0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders authHeaders(String token) {
        var h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private HttpHeaders authHeaders(String token, UUID idempotencyKey) {
        var h = authHeaders(token);
        h.set("Idempotency-Key", idempotencyKey.toString());
        return h;
    }

    private Map<String, Object> validReceiptBody(UUID clientOpId) {
        return Map.of(
                "supplierId", SUPPLIER_ID,
                "clientOperationId", clientOpId.toString(),
                "lines", List.of(Map.of(
                        "productId", PRODUCT_RICE_ID,
                        "expectedQuantity", "10.000",
                        "deliveredQuantity", "10.000",
                        "acceptedQuantity", "10.000",
                        "rejectedQuantity", "0.000",
                        "unitCost", 5000,
                        "expiryDate", LocalDate.now().plusDays(30).toString()
                )));
    }
}
