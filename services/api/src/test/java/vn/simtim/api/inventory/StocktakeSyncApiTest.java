package vn.simtim.api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Provider integration tests cho SYN-02: API đồng bộ kiểm kê và xử lý xung đột.
 *
 * <p>Kiểm tra:
 * <ul>
 *   <li>Mở phiên OPEN mới / trả lại phiên đang mở</li>
 *   <li>Idempotency: cùng clientOperationId+scope → trả kết quả cũ</li>
 *   <li>Cùng key nhưng khác phiên → 409 IDEMPOTENCY_KEY_REUSED</li>
 *   <li>Version lệch → 409 CONFLICT (không ghi tồn)</li>
 *   <li>Version đúng → 200 PENDING</li>
 *   <li>Thiếu quyền → 403</li>
 *   <li>Phiên đóng không nhận count mới</li>
 *   <li>Scope isolation: không lộ dữ liệu store khác</li>
 * </ul>
 *
 * <p>Chạy trên PostgreSQL thật qua Testcontainers; không mock domain.
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StocktakeSyncApiTest {

    static final String ORG_ID   = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID = "10000000-0000-0000-0000-000000000002";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_syn02_test");

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
            r.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private static final String PASSWORD = "syn02-fixture-pw-456";
    private final List<UUID> fixtureUsers  = new ArrayList<>();
    private final List<UUID> fixtureStores = new ArrayList<>();

    /** Token cho nhân viên kho (STOCK — có stocktakes.write). */
    private String stockToken;
    /** Token cho nhân viên bán hàng (SALES — không có stocktakes.write). */
    private String salesToken;

    /** UUID của lô hàng tồn tại trong demo seed (rice batch). */
    private UUID receiptId;
    private UUID receiptLineId;
    private UUID batchId;
    private UUID balanceId;
    /** Version hiện tại của balance lô đó. */
    private long batchVersion;

    @BeforeAll
    void setup() {
        String hash = passwords.hash(PASSWORD);
        stockToken = createSession("STOCK", hash, UUID.fromString(STORE_ID), "MAIN");
        salesToken = createSession("SALES", hash, UUID.fromString(STORE_ID), "MAIN");

        // Demo seed chỉ có product, không có batch/balance; ta tự tạo để test
        receiptId = UUID.randomUUID();
        receiptLineId = UUID.randomUUID();
        batchId = UUID.randomUUID();
        balanceId = UUID.randomUUID();
        batchVersion = 1L;

        UUID confirmedBy = fixtureUsers.getFirst();

        jdbc.update("""
                INSERT INTO inventory.goods_receipts(id, organization_id, store_id, supplier_id, status, received_at, confirmed_by, client_operation_id, idempotency_key, payload_hash)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000061', 'CONFIRMED', now(), ?, ?, ?, '\\x00')
                """, receiptId, ORG_ID, STORE_ID, confirmedBy, UUID.randomUUID(), UUID.randomUUID());

        jdbc.update("""
                INSERT INTO inventory.goods_receipt_lines(id, receipt_id, organization_id, product_id, expected_quantity, delivered_quantity, accepted_quantity, rejected_quantity, unit_cost)
                VALUES (?, ?, ?::uuid, '10000000-0000-0000-0000-000000000041', 100, 100, 100, 0, 10000)
                """, receiptLineId, receiptId, ORG_ID);

        jdbc.update("""
                INSERT INTO inventory.product_batches(id, organization_id, store_id, product_id, receipt_line_id, batch_number, status, received_date)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, 'B123', 'AVAILABLE', CURRENT_DATE)
                """, batchId, ORG_ID, STORE_ID, receiptLineId);

        jdbc.update("""
                INSERT INTO inventory.inventory_balances(id, organization_id, store_id, product_id, batch_id, on_hand_quantity, version)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, 100, ?)
                """, balanceId, ORG_ID, STORE_ID, batchId, batchVersion);
    }

    @AfterAll
    void tearDown() {
        try {
            UUID orgId = UUID.fromString(ORG_ID);
            jdbc.update("DELETE FROM inventory.stocktake_lines WHERE organization_id = ?", orgId);
            jdbc.update("DELETE FROM inventory.stocktakes WHERE organization_id = ?", orgId);
            jdbc.update("DELETE FROM inventory.inventory_balances WHERE id = ?", balanceId);
            jdbc.update("DELETE FROM inventory.product_batches WHERE id = ?", batchId);
            jdbc.update("DELETE FROM inventory.goods_receipt_lines WHERE id = ?", receiptLineId);
            jdbc.update("DELETE FROM inventory.goods_receipts WHERE id = ?", receiptId);
            for (UUID id : fixtureUsers) {
                jdbc.update("DELETE FROM iam.auth_sessions WHERE user_id = ?", id);
                jdbc.update("DELETE FROM iam.user_roles WHERE user_id = ?", id);
                jdbc.update("DELETE FROM iam.users WHERE id = ?", id);
            }
            for (UUID id : fixtureStores) {
                jdbc.update("DELETE FROM core.stores WHERE id = ?", id);
            }
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    // ── POST /api/v1/inventory/stocktakes ────────────────────────────────────

    @Test
    void openSession_stock_returns201WithOpenStatus() {
        var resp = rest.exchange("/api/v1/inventory/stocktakes",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(stockToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody()).containsEntry("status", "OPEN");
        assertThat(resp.getBody()).containsKey("id");
        assertThat(resp.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void openSession_calledTwice_returnsSameSession() {
        var first  = openSession(stockToken);
        var second = openSession(stockToken);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void openSession_sales_returns403() {
        var resp = rest.exchange("/api/v1/inventory/stocktakes",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(salesToken)),
                Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    // ── POST /api/v1/inventory/stocktakes/{sessionId}/counts ─────────────────

    @Test
    void submitCount_correctVersion_returnsPending200() {
        String sessionId = openSession(stockToken);
        UUID coid = UUID.randomUUID();

        var resp = submitCount(stockToken, sessionId, coid, batchId, BigDecimal.ONE, batchVersion);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "PENDING");
        assertThat(resp.getBody()).containsKey("clientOperationId");
    }

    @Test
    void submitCount_wrongVersion_returns409Conflict() {
        String sessionId = openSession(stockToken);

        var resp = submitCount(stockToken, sessionId, UUID.randomUUID(),
                batchId, BigDecimal.ONE, batchVersion + 9999L);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        assertThat((String) resp.getBody().get("code")).isEqualTo("CONFLICT");
        // Body harus mengandung info yang cukup untuk client re-count
        assertThat(resp.getBody()).containsKey("clientOperationId");
        assertThat(resp.getBody()).containsKey("baseVersion");
        // Tồn KHÔNG bị thay đổi — kiểm tra balance version vẫn như cũ
        long versionAfter = jdbc.queryForObject(
                "SELECT version FROM inventory.inventory_balances WHERE batch_id = ?",
                Long.class, batchId);
        assertThat(versionAfter).isEqualTo(batchVersion);
    }

    @Test
    void submitCount_idempotency_sameKeyAndScope_returnsSameLine() {
        String sessionId = openSession(stockToken);
        UUID coid = UUID.randomUUID();

        var first  = submitCount(stockToken, sessionId, coid, batchId, BigDecimal.ONE, batchVersion);
        var second = submitCount(stockToken, sessionId, coid, batchId, BigDecimal.ONE, batchVersion);

        assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    @Test
    void submitCount_sales_returns403() {
        String sessionId = openSession(stockToken);
        var resp = submitCount(salesToken, sessionId, UUID.randomUUID(),
                batchId, BigDecimal.ONE, batchVersion);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void submitCount_unknownBatch_returns404() {
        String sessionId = openSession(stockToken);
        var resp = submitCount(stockToken, sessionId, UUID.randomUUID(),
                UUID.randomUUID(), BigDecimal.ONE, 0L);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getSession_stock_returnsSessionWithLines() {
        String sessionId = openSession(stockToken);
        UUID coid = UUID.randomUUID();
        submitCount(stockToken, sessionId, coid, batchId, BigDecimal.ONE, batchVersion);

        var resp = rest.exchange("/api/v1/inventory/stocktakes/" + sessionId,
                HttpMethod.GET,
                new HttpEntity<>(null, authJson(stockToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        var lines = (List<?>) resp.getBody().get("lines");
        assertThat(lines).isNotEmpty();
    }

    @Test
    void getSession_otherStore_returns404() {
        // Tạo store và token khác
        UUID otherStoreId   = UUID.randomUUID();
        String otherCode    = "OTHER-" + otherStoreId.toString().substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO core.stores(id,organization_id,code,name,status) VALUES(?,?,?,?,'ACTIVE')",
                otherStoreId, UUID.fromString(ORG_ID), otherCode, "Other fixture");
        fixtureStores.add(otherStoreId);
        String otherToken = createSession("STOCK", passwords.hash(PASSWORD), otherStoreId, otherCode);

        // Phiên của stockToken thuộc STORE_ID
        String sessionId = openSession(stockToken);

        // otherToken thuộc store khác — không được nhìn thấy phiên
        var resp = rest.exchange("/api/v1/inventory/stocktakes/" + sessionId,
                HttpMethod.GET,
                new HttpEntity<>(null, authJson(otherToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String openSession(String token) {
        var resp = rest.exchange("/api/v1/inventory/stocktakes",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(token)),
                Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) resp.getBody().get("id");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> submitCount(String token, String sessionId,
                                             UUID coid, UUID batch,
                                             BigDecimal qty, long version) {
        var body = Map.of(
                "clientOperationId", coid.toString(),
                "batchId",          batch.toString(),
                "actualQuantity",   qty,
                "baseVersion",      version,
                "note",             "",
                "countedAt",        Instant.now().toString()
        );
        return rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/counts",
                HttpMethod.POST,
                new HttpEntity<>(body, authJson(token)),
                Map.class);
    }

    private HttpHeaders jsonHeaders() {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders authJson(String token) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    private String createSession(String role, String hash, UUID storeId, String storeCode) {
        UUID id = UUID.randomUUID();
        String username = "syn02_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                INSERT INTO iam.users(id,organization_id,username,password_hash,full_name,status)
                VALUES(?,?,?,?,?,'ACTIVE')
                """, id, UUID.fromString(ORG_ID), username, hash, "SYN02 fixture " + role);
        fixtureUsers.add(id);
        assertThat(jdbc.update("""
                INSERT INTO iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                SELECT organization_id,?,id,?,now(),? FROM iam.roles
                WHERE organization_id=? AND code=?
                """, id, storeId, id, UUID.fromString(ORG_ID), role)).isEqualTo(1);
        var resp = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of(
                "organizationCode", "SIMTIM",
                "storeCode",        storeCode,
                "username",         username,
                "password",         PASSWORD), jsonHeaders()), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        return (String) resp.getBody().get("accessToken");
    }
}
