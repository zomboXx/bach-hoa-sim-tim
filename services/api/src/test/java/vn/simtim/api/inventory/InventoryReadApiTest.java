package vn.simtim.api.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.simtim.api.auth.domain.PasswordHasher;
import vn.simtim.api.inventory.application.FefoPlan;
import vn.simtim.api.inventory.application.InventorySaleException;
import vn.simtim.api.inventory.application.InventorySalePort;
import vn.simtim.api.inventory.application.SaleIssue;
import vn.simtim.api.inventory.application.StockDemand;
import vn.simtim.api.inventory.application.StoreScope;

/** Provider tests cho read API INV-02 trên PostgreSQL thật. */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryReadApiTest {

    private static final UUID ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID MAIN_STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ALT_STORE_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SUPPLIER_ID = UUID.fromString("10000000-0000-0000-0000-000000000061");
    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000041");
    private static final String PASSWORD = "inv-read-fixture-password-123";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_inv_read_test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        String externalUrl = System.getenv("SIMTIM_TEST_DB_URL");
        if (externalUrl == null || externalUrl.isBlank()) {
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            registry.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;
    @Autowired InventorySalePort salePort;

    private String salesMainToken;
    private String salesAltToken;
    private UUID mainUserId;

    @BeforeAll
    void setup() {
        jdbc.update("""
                INSERT INTO core.stores(id, organization_id, code, name, status)
                VALUES (?, ?, 'ALT', 'Cửa hàng khác', 'ACTIVE')
                """, ALT_STORE_ID, ORG_ID);
        String hash = passwords.hash(PASSWORD);
        UUID mainUser = createUser("SALES", MAIN_STORE_ID, hash);
        mainUserId = mainUser;
        UUID altUser = createUser("SALES", ALT_STORE_ID, hash);
        salesMainToken = login(mainUser, "MAIN");
        salesAltToken = login(altUser, "ALT");

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        insertStoreStock(MAIN_STORE_ID, mainUser, today);
        insertAltStoreStock(ALT_STORE_ID, altUser, today);
    }

    @AfterAll
    void stop() {
        if (postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Test
    void products_includeBlockedAndExpiredOnHand_butOnlySellableStockIsAvailable() {
        Map<String, Object> body = get(
                "/api/v1/inventory/products?productId=" + PRODUCT_ID,
                salesMainToken);

        assertThat(body).containsEntry("totalElements", 1);
        Map<String, Object> product = firstItem(body);
        assertThat(product).containsEntry("productId", PRODUCT_ID.toString());
        assertThat(new BigDecimal(product.get("onHandQuantity").toString()))
                .isEqualByComparingTo("21.000");
        assertThat(new BigDecimal(product.get("availableQuantity").toString()))
                .isEqualByComparingTo("14.000");
        assertThat(product).doesNotContainKey("unitCost");
        assertThat(body.toString()).doesNotContain("unitCost");
    }

    @Test
    void batches_classifyYesterdayTodayPlusSevenPlusEightNull_andBlocked() {
        Map<String, Object> body = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&size=100",
                salesMainToken);
        Map<String, Map<String, Object>> byNumber = new HashMap<>();
        for (Map<String, Object> item : items(body)) {
            byNumber.put(item.get("batchNumber").toString(), item);
        }

        assertThat(byNumber).hasSize(6);
        assertThat(byNumber.get("YESTERDAY")).containsEntry("expiryStatus", "EXPIRED");
        assertThat(byNumber.get("TODAY")).containsEntry("expiryStatus", "NEAR_EXPIRY");
        assertThat(byNumber.get("PLUS-7")).containsEntry("expiryStatus", "NEAR_EXPIRY");
        assertThat(byNumber.get("PLUS-8")).containsEntry("expiryStatus", "VALID");
        assertThat(byNumber.get("NO-EXPIRY")).containsEntry("expiryStatus", "NO_EXPIRY");
        assertThat(byNumber.get("BLOCKED")).containsEntry("status", "BLOCKED");
        assertThat(new BigDecimal(byNumber.get("YESTERDAY").get("availableQuantity").toString()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(new BigDecimal(byNumber.get("TODAY").get("availableQuantity").toString()))
                .isEqualByComparingTo("2.000");
        assertThat(new BigDecimal(byNumber.get("BLOCKED").get("availableQuantity").toString()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void batches_filterAndPagination_haveDeterministicNonOverlappingOrder() {
        Map<String, Object> first = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&page=0&size=2",
                salesMainToken);
        Map<String, Object> repeated = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&page=0&size=2",
                salesMainToken);
        Map<String, Object> second = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&page=1&size=2",
                salesMainToken);

        List<String> firstIds = itemIds(first, "batchId");
        assertThat(itemIds(repeated, "batchId")).isEqualTo(firstIds);
        assertThat(new HashSet<>(itemIds(second, "batchId")))
                .doesNotContainAnyElementsOf(firstIds);

        Map<String, Object> blocked = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&status=BLOCKED",
                salesMainToken);
        assertThat(blocked).containsEntry("totalElements", 1);
        assertThat(firstItem(blocked)).containsEntry("batchNumber", "BLOCKED");

        Map<String, Object> expired = get(
                "/api/v1/inventory/batches?productId=" + PRODUCT_ID + "&expiryStatus=EXPIRED",
                salesMainToken);
        assertThat(expired).containsEntry("totalElements", 1);
        assertThat(firstItem(expired)).containsEntry("batchNumber", "YESTERDAY");
    }

    @Test
    void movements_exposeReceiptSourceAndDelta_withoutCost() {
        Map<String, Object> body = get(
                "/api/v1/inventory/movements?productId=" + PRODUCT_ID + "&type=RECEIPT&size=100",
                salesMainToken);

        assertThat(body).containsEntry("totalElements", 6);
        for (Map<String, Object> movement : items(body)) {
            assertThat(new BigDecimal(movement.get("quantityDelta").toString()))
                    .isGreaterThan(BigDecimal.ZERO);
            Map<String, Object> source = castMap(movement.get("source"));
            assertThat(source).containsEntry("type", "GOODS_RECEIPT");
            assertThat(source.get("id")).isNotNull();
        }
        assertThat(body.toString()).doesNotContain("unitCost");
    }

    @Test
    void sessionScope_neverReturnsAnotherStoreStock() {
        Map<String, Object> main = get(
                "/api/v1/inventory/products?productId=" + PRODUCT_ID,
                salesMainToken);
        Map<String, Object> alt = get(
                "/api/v1/inventory/products?productId=" + PRODUCT_ID,
                salesAltToken);

        assertThat(new BigDecimal(firstItem(main).get("onHandQuantity").toString()))
                .isEqualByComparingTo("21.000");
        assertThat(new BigDecimal(firstItem(alt).get("onHandQuantity").toString()))
                .isEqualByComparingTo("99.000");
    }

    @Test
    void missingSessionIs401_andInvalidFilterIs400() {
        var unauthenticated = rest.exchange(
                "/api/v1/inventory/products", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), Map.class);
        assertThat(unauthenticated.getStatusCode().value()).isEqualTo(401);

        var invalid = rest.exchange(
                "/api/v1/inventory/batches?status=UNKNOWN", HttpMethod.GET,
                new HttpEntity<>(authHeaders(salesMainToken)), Map.class);
        assertThat(invalid.getStatusCode().value()).isEqualTo(400);
        assertThat(invalid.getBody()).containsEntry("code", "INVALID_REQUEST");
    }

    @Test
    @Transactional
    void salePort_plansFefoThenDeductsAndRecordsMovementsInCallerTransaction() {
        StoreScope scope = new StoreScope(ORG_ID, MAIN_STORE_ID);
        FefoPlan plan = salePort.planForCheckout(
                scope, List.of(new StockDemand(PRODUCT_ID, new BigDecimal("8.000"))));

        assertThat(plan.businessDate()).isEqualTo(LocalDate.now(BUSINESS_ZONE));
        assertThat(plan.allocations()).hasSize(3);
        assertThat(plan.allocations())
                .extracting(allocation -> allocation.expiryDate())
                .containsExactly(
                        plan.businessDate(),
                        plan.businessDate().plusDays(7),
                        plan.businessDate().plusDays(8));
        assertThat(plan.allocations())
                .extracting(allocation -> allocation.quantity())
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(
                        new BigDecimal("2.000"),
                        new BigDecimal("3.000"),
                        new BigDecimal("3.000"));

        UUID invoiceLineId = UUID.randomUUID();
        List<SaleIssue> issues = plan.allocations().stream()
                .map(allocation -> new SaleIssue(
                        UUID.randomUUID(), invoiceLineId,
                        allocation.productId(), allocation.batchId(),
                        allocation.quantity(), mainUserId))
                .toList();
        salePort.postSale(plan, issues);

        BigDecimal total = jdbc.queryForObject("""
                SELECT sum(on_hand_quantity)
                FROM inventory.inventory_balances
                WHERE organization_id=? AND store_id=? AND product_id=?
                """, BigDecimal.class, ORG_ID, MAIN_STORE_ID, PRODUCT_ID);
        assertThat(total).isEqualByComparingTo("13.000");
        Integer saleMovements = jdbc.queryForObject("""
                SELECT count(*) FROM inventory.stock_movements
                WHERE organization_id=? AND store_id=?
                  AND reference_id=? AND reference_type='INVOICE' AND movement_type='SALE'
                """, Integer.class, ORG_ID, MAIN_STORE_ID, invoiceLineId);
        assertThat(saleMovements).isEqualTo(3);
    }

    @Test
    @Transactional
    void salePort_rejectsDemandBeyondAvailableStock() {
        assertThatThrownBy(() -> salePort.planForCheckout(
                new StoreScope(ORG_ID, MAIN_STORE_ID),
                List.of(new StockDemand(PRODUCT_ID, new BigDecimal("15.000")))))
                .isInstanceOf(InventorySaleException.class)
                .extracting(error -> ((InventorySaleException) error).getCode())
                .isEqualTo("INSUFFICIENT_STOCK");
    }

    private UUID createUser(String role, UUID storeId, String hash) {
        UUID id = UUID.randomUUID();
        String username = "inv_read_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                INSERT INTO iam.users(id, organization_id, username, password_hash, full_name, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                """, id, ORG_ID, username, hash, "INV-02 " + role);
        jdbc.update("""
                INSERT INTO iam.user_roles(
                    organization_id, user_id, role_id, store_id, assigned_at, assigned_by)
                SELECT organization_id, ?, id, ?, now(), ?
                FROM iam.roles WHERE organization_id = ? AND code = ?
                """, id, storeId, id, ORG_ID, role);
        return id;
    }

    private String login(UUID userId, String storeCode) {
        String username = jdbc.queryForObject(
                "SELECT username FROM iam.users WHERE id=?", String.class, userId);
        var response = rest.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(Map.of(
                        "organizationCode", "SIMTIM", "storeCode", storeCode,
                        "username", username, "password", PASSWORD), jsonHeaders()),
                Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody().get("accessToken").toString();
    }

    private void insertStoreStock(UUID storeId, UUID actorId, LocalDate today) {
        UUID receiptId = insertReceipt(storeId, actorId);
        UUID lineId = insertReceiptLine(receiptId);
        insertBatch(storeId, actorId, receiptId, lineId,
                "YESTERDAY", "AVAILABLE", today.minusDays(1), new BigDecimal("1.000"), 1);
        insertBatch(storeId, actorId, receiptId, lineId,
                "TODAY", "AVAILABLE", today, new BigDecimal("2.000"), 2);
        insertBatch(storeId, actorId, receiptId, lineId,
                "PLUS-7", "AVAILABLE", today.plusDays(7), new BigDecimal("3.000"), 3);
        insertBatch(storeId, actorId, receiptId, lineId,
                "PLUS-8", "AVAILABLE", today.plusDays(8), new BigDecimal("4.000"), 4);
        insertBatch(storeId, actorId, receiptId, lineId,
                "NO-EXPIRY", "AVAILABLE", null, new BigDecimal("5.000"), 5);
        insertBatch(storeId, actorId, receiptId, lineId,
                "BLOCKED", "BLOCKED", today.plusDays(8), new BigDecimal("6.000"), 6);
    }

    private void insertAltStoreStock(UUID storeId, UUID actorId, LocalDate today) {
        UUID receiptId = insertReceipt(storeId, actorId);
        UUID lineId = insertReceiptLine(receiptId);
        insertBatch(storeId, actorId, receiptId, lineId,
                "ALT-ONLY", "AVAILABLE", today.plusDays(30), new BigDecimal("99.000"), 1);
    }

    private UUID insertReceipt(UUID storeId, UUID actorId) {
        UUID receiptId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory.goods_receipts(
                    id, organization_id, store_id, supplier_id, status, received_at,
                    confirmed_by, client_operation_id, idempotency_key, payload_hash)
                VALUES (?, ?, ?, ?, 'CONFIRMED', now(), ?, ?, ?, ?)
                """, receiptId, ORG_ID, storeId, SUPPLIER_ID, actorId,
                UUID.randomUUID(), UUID.randomUUID(), new byte[32]);
        return receiptId;
    }

    private UUID insertReceiptLine(UUID receiptId) {
        UUID lineId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory.goods_receipt_lines(
                    id, receipt_id, organization_id, product_id,
                    expected_quantity, delivered_quantity, accepted_quantity,
                    rejected_quantity, unit_cost)
                VALUES (?, ?, ?, ?, 120, 120, 120, 0, 12345)
                """, lineId, receiptId, ORG_ID, PRODUCT_ID);
        return lineId;
    }

    private void insertBatch(
            UUID storeId, UUID actorId, UUID receiptId, UUID lineId,
            String batchNumber, String status, LocalDate expiryDate,
            BigDecimal quantity, int sequence) {
        UUID batchId = UUID.randomUUID();
        UUID balanceId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inventory.product_batches(
                    id, organization_id, store_id, product_id, receipt_line_id,
                    batch_number, expiry_date, received_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, ORG_ID, storeId, PRODUCT_ID, lineId,
                batchNumber, expiryDate, LocalDate.now(BUSINESS_ZONE).minusDays(10 - sequence), status);
        jdbc.update("""
                INSERT INTO inventory.inventory_balances(
                    id, organization_id, store_id, product_id, batch_id, on_hand_quantity)
                VALUES (?, ?, ?, ?, ?, ?)
                """, balanceId, ORG_ID, storeId, PRODUCT_ID, batchId, quantity);
        jdbc.update("""
                INSERT INTO inventory.stock_movements(
                    id, organization_id, store_id, product_id, batch_id,
                    movement_type, quantity_delta, reference_id, reference_type,
                    occurred_at, recorded_by)
                VALUES (?, ?, ?, ?, ?, 'RECEIPT', ?, ?, 'GOODS_RECEIPT', ?, ?)
                """, UUID.randomUUID(), ORG_ID, storeId, PRODUCT_ID, batchId,
                quantity, receiptId, Instant.now().minusSeconds(sequence), actorId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String token) {
        var response = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("items");
    }

    private Map<String, Object> firstItem(Map<String, Object> body) {
        assertThat(items(body)).isNotEmpty();
        return items(body).getFirst();
    }

    private List<String> itemIds(Map<String, Object> body, String field) {
        return items(body).stream().map(item -> item.get(field).toString()).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
