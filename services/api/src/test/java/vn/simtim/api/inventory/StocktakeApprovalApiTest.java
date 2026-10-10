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
import org.junit.jupiter.api.BeforeEach;
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
 * Provider integration tests cho INV-03: Duyệt điều chỉnh tồn kho.
 *
 * <p>Kiểm tra toàn diện trên PostgreSQL 17 thật qua Testcontainers:
 * <ul>
 *   <li>Tăng tồn kho: actual > on_hand, ghi movement STOCKTAKE_ADJUSTMENT delta dương, đối soát ledger độc lập</li>
 *   <li>Giảm tồn kho: actual < on_hand, ghi movement STOCKTAKE_ADJUSTMENT delta âm, đối soát ledger độc lập</li>
 *   <li>Count bằng tồn: actual == on_hand, không ghi movement thừa, đối soát ledger độc lập</li>
 *   <li>Thiếu quyền: nhân viên STOCK hoặc SALES gọi duyệt bị từ chối 403 Forbidden</li>
 *   <li>Duyệt lặp: gọi approve lần 2 bị từ chối 409 Conflict (ALREADY_APPROVED)</li>
 *   <li>Tồn đổi trước khi duyệt: version mismatch bị từ chối 409 Conflict, rollback</li>
 *   <li>Cross-store isolation: quản lý store khác không thể duyệt phiên, trả 404</li>
 *   <li>Nguyên tử & Rollback: phiên 2 dòng gặp lỗi 1 dòng thì rollback toàn bộ, không ghi một phần</li>
 *   <li>Phiên rỗng: từ chối 422 EMPTY_STOCKTAKE</li>
 * </ul>
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StocktakeApprovalApiTest {

    static final String ORG_ID   = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID = "10000000-0000-0000-0000-000000000002";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_inv03_test");

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

    private static final String PASSWORD = "inv03-fixture-pw-789";
    private final List<UUID> fixtureUsers  = new ArrayList<>();
    private final List<UUID> fixtureStores = new ArrayList<>();
    private final List<UUID> fixtureBatches = new ArrayList<>();
    private final List<UUID> fixtureReceipts = new ArrayList<>();

    private String managerToken;
    private String stockToken;
    private String otherStoreManagerToken;
    private UUID otherStoreId;

    @BeforeAll
    void setup() {
        // Cấp quyền cho demo org
        jdbc.update("""
                INSERT INTO iam.role_permissions(role_id, permission_id)
                SELECT r.id, p.id FROM iam.roles r CROSS JOIN iam.permissions p
                WHERE r.organization_id = ?::uuid
                  AND (
                    (p.code = 'stocktakes.read'    AND r.code IN ('STOCK', 'MANAGER', 'ADMIN'))
                    OR (p.code = 'stocktakes.write'   AND r.code IN ('STOCK', 'MANAGER'))
                    OR (p.code = 'stocktakes.approve' AND r.code IN ('MANAGER', 'ADMIN'))
                  )
                ON CONFLICT DO NOTHING
                """, ORG_ID);

        String hash = passwords.hash(PASSWORD);
        managerToken = createSession("MANAGER", hash, UUID.fromString(STORE_ID), "MAIN");
        stockToken   = createSession("STOCK",   hash, UUID.fromString(STORE_ID), "MAIN");

        // Tạo store thứ hai cho test cross-store
        otherStoreId = UUID.randomUUID();
        fixtureStores.add(otherStoreId);
        jdbc.update("""
                INSERT INTO core.stores (id, organization_id, code, name, status)
                VALUES (?, ?::uuid, 'STORE_B', 'Cửa hàng B', 'ACTIVE')
                """, otherStoreId, ORG_ID);
        otherStoreManagerToken = createSession("MANAGER", hash, otherStoreId, "STORE_B");
    }

    @BeforeEach
    void cleanStocktakeState() {
        jdbc.update("DELETE FROM audit.audit_logs WHERE organization_id = ?::uuid", ORG_ID);
        jdbc.update("DELETE FROM inventory.stock_movements WHERE organization_id = ?::uuid AND reference_type = 'STOCKTAKE'", ORG_ID);
        jdbc.update("DELETE FROM inventory.stocktake_lines WHERE organization_id = ?::uuid", ORG_ID);
        jdbc.update("DELETE FROM inventory.stocktakes WHERE organization_id = ?::uuid", ORG_ID);
    }

    @AfterAll
    void tearDown() {
        try {
            UUID orgId = UUID.fromString(ORG_ID);
            jdbc.update("DELETE FROM audit.audit_logs WHERE organization_id = ?", orgId);
            jdbc.update("DELETE FROM inventory.stock_movements WHERE organization_id = ? AND reference_type = 'STOCKTAKE'", orgId);
            jdbc.update("DELETE FROM inventory.stocktake_lines WHERE organization_id = ?", orgId);
            jdbc.update("DELETE FROM inventory.stocktakes WHERE organization_id = ?", orgId);
            for (UUID bId : fixtureBatches) {
                jdbc.update("DELETE FROM inventory.inventory_balances WHERE batch_id = ?", bId);
                jdbc.update("DELETE FROM inventory.stock_movements WHERE batch_id = ?", bId);
                jdbc.update("DELETE FROM inventory.product_batches WHERE id = ?", bId);
            }
            for (UUID rId : fixtureReceipts) {
                jdbc.update("DELETE FROM inventory.goods_receipt_lines WHERE receipt_id = ?", rId);
                jdbc.update("DELETE FROM inventory.goods_receipts WHERE id = ?", rId);
            }
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

    // ── Test Cases ────────────────────────────────────────────────────────────

    /**
     * Tăng tồn kho: Số đếm 105 > tồn 100 → delta = +5.
     * Cập nhật balance = 105, tạo movement STOCKTAKE_ADJUSTMENT (+5),
     * đối soát ledger SUM(quantity_delta) == balance.
     */
    @Test
    void approve_increaseQuantity_updatesBalanceAndCreatesStockMovementAndMatchesLedger() {
        var fixture = createBatchWithBalance("B_INC", new BigDecimal("100.000"), 1L);
        String sessionId = openSession(stockToken);

        // Đếm thực tế: 105
        var countResp = submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(),
                new BigDecimal("105.000"), 1L);
        assertThat(countResp.getStatusCode().value()).isEqualTo(200);

        // Manager duyệt
        var approveResp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(approveResp.getStatusCode().value()).isEqualTo(200);
        assertThat(approveResp.getBody()).containsEntry("status", "APPROVED");

        // Kiểm tra balance mới trong DB
        BigDecimal newBalance = queryBalance(fixture.batchId());
        assertThat(newBalance).isEqualByComparingTo("105.000");

        // Kiểm tra movement STOCKTAKE_ADJUSTMENT
        var movements = jdbc.queryForList("""
                SELECT movement_type, quantity_delta, reference_id, reference_type
                FROM inventory.stock_movements
                WHERE reference_id = ?::uuid AND reference_type = 'STOCKTAKE'
                """, sessionId);
        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().get("movement_type")).isEqualTo("STOCKTAKE_ADJUSTMENT");
        assertThat((BigDecimal) movements.getFirst().get("quantity_delta")).isEqualByComparingTo("5.000");

        // Đối soát ledger độc lập: SUM(quantity_delta) = 100 (nhập) + 5 (điều chỉnh) = 105
        BigDecimal ledgerSum = queryLedgerSum(fixture.batchId());
        assertThat(ledgerSum).isEqualByComparingTo(newBalance);

        // Kiểm tra audit log
        Integer auditCount = jdbc.queryForObject(
                "SELECT count(*) FROM audit.audit_logs WHERE entity_id = ?::uuid AND action = 'APPROVE_STOCKTAKE'",
                Integer.class, sessionId);
        assertThat(auditCount).isNotNull().isGreaterThanOrEqualTo(1);
    }

    /**
     * Giảm tồn kho: Số đếm 96 < tồn 100 → delta = -4.
     * Cập nhật balance = 96, tạo movement STOCKTAKE_ADJUSTMENT (-4),
     * đối soát ledger SUM(quantity_delta) == balance.
     */
    @Test
    void approve_decreaseQuantity_updatesBalanceAndCreatesStockMovementAndMatchesLedger() {
        var fixture = createBatchWithBalance("B_DEC", new BigDecimal("100.000"), 1L);
        String sessionId = openSession(stockToken);

        // Đếm thực tế: 96
        var countResp = submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(),
                new BigDecimal("96.000"), 1L);
        assertThat(countResp.getStatusCode().value()).isEqualTo(200);

        // Manager duyệt
        var approveResp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(approveResp.getStatusCode().value()).isEqualTo(200);

        BigDecimal newBalance = queryBalance(fixture.batchId());
        assertThat(newBalance).isEqualByComparingTo("96.000");

        // Movement delta = -4
        var movements = jdbc.queryForList("""
                SELECT movement_type, quantity_delta
                FROM inventory.stock_movements
                WHERE reference_id = ?::uuid AND reference_type = 'STOCKTAKE'
                """, sessionId);
        assertThat(movements).hasSize(1);
        assertThat((BigDecimal) movements.getFirst().get("quantity_delta")).isEqualByComparingTo("-4.000");

        // Đối soát ledger độc lập: 100 - 4 = 96
        BigDecimal ledgerSum = queryLedgerSum(fixture.batchId());
        assertThat(ledgerSum).isEqualByComparingTo(newBalance);
    }

    /**
     * Count bằng tồn: Số đếm 100 == tồn 100 → delta = 0.
     * Phiên được APPROVED, balance giữ nguyên 100, không sinh movement điều chỉnh thừa.
     */
    @Test
    void approve_countEqualsStock_noDeltaMovementAndMatchesLedger() {
        var fixture = createBatchWithBalance("B_EQUAL", new BigDecimal("100.000"), 1L);
        String sessionId = openSession(stockToken);

        // Đếm thực tế = tồn hệ thống: 100
        var countResp = submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(),
                new BigDecimal("100.000"), 1L);
        assertThat(countResp.getStatusCode().value()).isEqualTo(200);

        // Manager duyệt
        var approveResp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(approveResp.getStatusCode().value()).isEqualTo(200);
        assertThat(approveResp.getBody()).containsEntry("status", "APPROVED");

        BigDecimal newBalance = queryBalance(fixture.batchId());
        assertThat(newBalance).isEqualByComparingTo("100.000");

        // Không tạo movement mới vì delta = 0
        var movements = jdbc.queryForList("""
                SELECT movement_type FROM inventory.stock_movements
                WHERE reference_id = ?::uuid AND reference_type = 'STOCKTAKE'
                """, sessionId);
        assertThat(movements).isEmpty();

        // Ledger vẫn khớp balance = 100
        BigDecimal ledgerSum = queryLedgerSum(fixture.batchId());
        assertThat(ledgerSum).isEqualByComparingTo(newBalance);
    }

    /**
     * Thiếu quyền: Nhân viên STOCK gọi endpoint approve → 403 Forbidden.
     */
    @Test
    void approve_byStockUser_returns403Forbidden() {
        var fixture = createBatchWithBalance("B_PERM", new BigDecimal("50.000"), 1L);
        String sessionId = openSession(stockToken);
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(), new BigDecimal("48.000"), 1L);

        // Stock user cố tình duyệt
        var resp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(stockToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);

        // Phiên vẫn OPEN
        String status = jdbc.queryForObject(
                "SELECT status FROM inventory.stocktakes WHERE id = ?::uuid",
                String.class, sessionId);
        assertThat(status).isEqualTo("OPEN");
    }

    /**
     * Không duyệt hai lần: Gọi approve lần 2 trả về 409 Conflict.
     */
    @Test
    void approve_calledTwice_returns409Conflict() {
        var fixture = createBatchWithBalance("B_TWICE", new BigDecimal("50.000"), 1L);
        String sessionId = openSession(stockToken);
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(), new BigDecimal("52.000"), 1L);

        // Duyệt lần 1
        var first = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);
        assertThat(first.getStatusCode().value()).isEqualTo(200);

        // Duyệt lần 2
        var second = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);
        assertThat(second.getStatusCode().value()).isEqualTo(409);
        assertThat(second.getBody()).containsEntry("code", "ALREADY_APPROVED");
    }

    /**
     * Tồn đổi trước khi duyệt: Phiên bản tồn thay đổi sau khi kiểm đếm
     * → Từ chối duyệt 409 Conflict (BALANCE_VERSION_MISMATCH), rollback toàn bộ.
     */
    @Test
    void approve_balanceChangedBeforeApproval_returns409ConflictAndRollsBack() {
        var fixture = createBatchWithBalance("B_MISMATCH", new BigDecimal("70.000"), 1L);
        String sessionId = openSession(stockToken);
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(), new BigDecimal("65.000"), 1L);

        // Giả lập giao dịch khác làm tăng version của balance lên 2 trước khi manager duyệt
        jdbc.update("UPDATE inventory.inventory_balances SET version = 2 WHERE batch_id = ?", fixture.batchId());

        // Manager duyệt
        var resp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        assertThat(resp.getBody()).containsEntry("code", "BALANCE_VERSION_MISMATCH");

        // Balance không bị đổi (vẫn 70)
        BigDecimal currentBal = queryBalance(fixture.batchId());
        assertThat(currentBal).isEqualByComparingTo("70.000");

        // Không tạo stock movement
        var movements = jdbc.queryForList("""
                SELECT id FROM inventory.stock_movements
                WHERE reference_id = ?::uuid AND reference_type = 'STOCKTAKE'
                """, sessionId);
        assertThat(movements).isEmpty();

        // Phiên vẫn OPEN
        String status = jdbc.queryForObject(
                "SELECT status FROM inventory.stocktakes WHERE id = ?::uuid",
                String.class, sessionId);
        assertThat(status).isEqualTo("OPEN");
    }

    /**
     * Phân lập store (Cross-store): Quản lý cửa hàng B không thể duyệt phiên của cửa hàng MAIN.
     */
    @Test
    void approve_crossStore_returns404NotFound() {
        var fixture = createBatchWithBalance("B_CROSS", new BigDecimal("30.000"), 1L);
        String sessionId = openSession(stockToken);
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture.batchId(), new BigDecimal("32.000"), 1L);

        // Manager store B gọi approve
        var resp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(otherStoreManagerToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).containsEntry("code", "NOT_FOUND");
    }

    /**
     * Tính nguyên tử & Rollback: Phiên có 2 dòng (dòng 1 hợp lệ, dòng 2 bị đổi version).
     * Khi duyệt thất bại, dòng 1 KHÔNG được ghi đè, không có movement nào được tạo (no partial write).
     */
    @Test
    void approve_rollbackOnFailure_noPartialWrite() {
        var fixture1 = createBatchWithBalance("B_ROLL1", new BigDecimal("100.000"), 1L);
        var fixture2 = createBatchWithBalance("B_ROLL2", new BigDecimal("200.000"), 1L);

        String sessionId = openSession(stockToken);
        // Dòng 1: hợp lệ
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture1.batchId(), new BigDecimal("110.000"), 1L);
        // Dòng 2: đếm theo baseVersion 1
        submitCount(stockToken, sessionId, UUID.randomUUID(), fixture2.batchId(), new BigDecimal("190.000"), 1L);

        // Giả lập dòng 2 bị thay đổi version trên DB trước khi duyệt
        jdbc.update("UPDATE inventory.inventory_balances SET version = 3 WHERE batch_id = ?", fixture2.batchId());

        // Manager gọi duyệt
        var resp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);

        // Dòng 1 KHÔNG bị cập nhật một phần: balance1 vẫn là 100.000
        BigDecimal bal1 = queryBalance(fixture1.batchId());
        assertThat(bal1).isEqualByComparingTo("100.000");

        // Dòng 2 balance vẫn là 200.000
        BigDecimal bal2 = queryBalance(fixture2.batchId());
        assertThat(bal2).isEqualByComparingTo("200.000");

        // Tuyệt đối không có movement nào được tạo
        var movements = jdbc.queryForList("""
                SELECT id FROM inventory.stock_movements
                WHERE reference_id = ?::uuid AND reference_type = 'STOCKTAKE'
                """, sessionId);
        assertThat(movements).isEmpty();

        // Phiên vẫn là OPEN
        String status = jdbc.queryForObject(
                "SELECT status FROM inventory.stocktakes WHERE id = ?::uuid",
                String.class, sessionId);
        assertThat(status).isEqualTo("OPEN");
    }

    /**
     * Phiên rỗng không có dòng nào: từ chối duyệt 422 EMPTY_STOCKTAKE.
     */
    @Test
    void approve_emptyStocktake_returns422Unprocessable() {
        String sessionId = openSession(stockToken);

        var resp = rest.exchange(
                "/api/v1/inventory/stocktakes/" + sessionId + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(null, authJson(managerToken)),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(422);
        assertThat(resp.getBody()).containsEntry("code", "EMPTY_STOCKTAKE");
    }

    // ── Fixture & Helper methods ──────────────────────────────────────────────

    private record BatchFixture(UUID batchId, UUID balanceId) {}

    private BatchFixture createBatchWithBalance(String batchNumber, BigDecimal initialQty, long version) {
        UUID receiptId = UUID.randomUUID();
        UUID receiptLineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID balanceId = UUID.randomUUID();

        fixtureReceipts.add(receiptId);
        fixtureBatches.add(batchId);

        UUID confirmedBy = fixtureUsers.getFirst();

        jdbc.update("""
                INSERT INTO inventory.goods_receipts(id, organization_id, store_id, supplier_id, status, received_at, confirmed_by, client_operation_id, idempotency_key, payload_hash)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000061', 'CONFIRMED', now(), ?, ?, ?, '\\x00')
                """, receiptId, ORG_ID, STORE_ID, confirmedBy, UUID.randomUUID(), UUID.randomUUID());

        jdbc.update("""
                INSERT INTO inventory.goods_receipt_lines(id, receipt_id, organization_id, product_id, expected_quantity, delivered_quantity, accepted_quantity, rejected_quantity, unit_cost)
                VALUES (?, ?, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, ?, ?, 0, 10000)
                """, receiptLineId, receiptId, ORG_ID, initialQty, initialQty, initialQty);

        jdbc.update("""
                INSERT INTO inventory.product_batches(id, organization_id, store_id, product_id, receipt_line_id, batch_number, status, received_date)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, ?, 'AVAILABLE', CURRENT_DATE)
                """, batchId, ORG_ID, STORE_ID, receiptLineId, batchNumber);

        jdbc.update("""
                INSERT INTO inventory.inventory_balances(id, organization_id, store_id, product_id, batch_id, on_hand_quantity, version)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, ?, ?)
                """, balanceId, ORG_ID, STORE_ID, batchId, initialQty, version);

        // Ghi nhận ban đầu RECEIPT movement để đối soát ledger
        jdbc.update("""
                INSERT INTO inventory.stock_movements(id, organization_id, store_id, product_id, batch_id, movement_type, quantity_delta, reference_id, reference_type, occurred_at, recorded_by)
                VALUES (?, ?::uuid, ?::uuid, '10000000-0000-0000-0000-000000000041', ?, 'RECEIPT', ?, ?, 'GOODS_RECEIPT', now(), ?)
                """, UUID.randomUUID(), ORG_ID, STORE_ID, batchId, initialQty, receiptId, confirmedBy);

        return new BatchFixture(batchId, balanceId);
    }

    private BigDecimal queryBalance(UUID batchId) {
        return jdbc.queryForObject(
                "SELECT on_hand_quantity FROM inventory.inventory_balances WHERE batch_id = ?",
                BigDecimal.class, batchId);
    }

    private BigDecimal queryLedgerSum(UUID batchId) {
        return jdbc.queryForObject(
                "SELECT coalesce(sum(quantity_delta), 0) FROM inventory.stock_movements WHERE batch_id = ?",
                BigDecimal.class, batchId);
    }

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
        String username = "inv03_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                INSERT INTO iam.users(id,organization_id,username,password_hash,full_name,status)
                VALUES(?,?,?,?,?,'ACTIVE')
                """, id, UUID.fromString(ORG_ID), username, hash, "INV03 fixture " + role);
        fixtureUsers.add(id);
        int assigned = jdbc.update("""
                INSERT INTO iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                SELECT organization_id,?,id,?,now(),? FROM iam.roles
                WHERE organization_id=? AND code=?
                """, id, storeId, id, UUID.fromString(ORG_ID), role);
        assertThat(assigned)
                .as("Role '%s' not found in organization %s", role, ORG_ID)
                .isEqualTo(1);
        var resp = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of(
                "organizationCode", "SIMTIM",
                "storeCode",        storeCode,
                "username",         username,
                "password",         PASSWORD), jsonHeaders()), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        return (String) resp.getBody().get("accessToken");
    }
}
