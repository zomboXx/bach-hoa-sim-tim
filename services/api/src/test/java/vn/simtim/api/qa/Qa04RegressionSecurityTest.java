package vn.simtim.api.qa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.simtim.api.auth.domain.PasswordHasher;

/**
 * QA-04 independent cross-module regression, authorization and backup/restore gate.
 *
 * <p>Validates:
 * <ul>
 *   <li>Full cross-module regression: Goods receipt (INV-01), inventory read/FEFO (INV-02),
 *       stocktake sync (SYN-02) and approval (INV-03), sales quote & cash checkout with
 *       promotions (SAL-01, PRO-01B), and SQL oracle reconciliation for revenue and inventory reports (REP-01).</li>
 *   <li>Stock ledger integrity invariant: on_hand_quantity = SUM(stock_movements.quantity_delta).</li>
 *   <li>Authorization & RBAC matrix: 4 roles (SALES, STOCK, MANAGER, ADMIN) + anonymous 401,
 *       deny-by-default, and sensitive purchase cost masking for SALES.</li>
 *   <li>Store scope isolation: preventing cross-store data leakage or mutation.</li>
 *   <li>Invoice privacy behavior: documenting current S2-GAP-03 visibility for SALES in the same store.</li>
 *   <li>Training isolation: session principal carries trainingEnabled flag without contaminating operational tables.</li>
 *   <li>Transaction safety: cart rollback on insufficient inventory and concurrency serialization on the last unit.</li>
 *   <li>Backup & restore on a clean PostgreSQL environment (NFR-BAC-01): pg_dump, clean database restore
 *       with -v ON_ERROR_STOP=1, failure on corrupted SQL dump, and full entity/invariant/oracle reconciliation.</li>
 * </ul>
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Qa04RegressionSecurityTest {

    private static final UUID ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID SUPPLIER_ID = UUID.fromString("10000000-0000-0000-0000-000000000061");
    private static final UUID RICE_ID = UUID.fromString("10000000-0000-0000-0000-000000000041"); // EA, 25,000 VND
    private static final UUID APPLE_ID = UUID.fromString("10000000-0000-0000-0000-000000000042"); // KG, 40,000 VND
    private static final UUID PROMO_RICE_ID = UUID.fromString("10000000-0000-0000-0000-000000000081");
    private static final String PASSWORD = "qa04-disposable-password";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine").withDatabaseName("simtim_qa04_test");

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
    private String salesToken2;
    private String stockToken;
    private String managerToken;
    private String adminToken;
    private String trainingUserToken;

    @BeforeAll
    void createActors() {
        salesToken = createActor("SALES", false);
        salesToken2 = createActor("SALES", false);
        stockToken = createActor("STOCK", false);
        managerToken = createActor("MANAGER", false);
        adminToken = createActor("ADMIN", false);
        trainingUserToken = createActor("SALES", true);

        // Ensure secondary store exists for scope isolation testing
        jdbc.update("""
                insert into core.stores (id, organization_id, code, name, status)
                values (?, ?, 'STORE_B', 'Cửa hàng B', 'ACTIVE')
                on conflict (id) do nothing
                """, OTHER_STORE_ID, ORG_ID);
    }

    @BeforeEach
    void resetBusinessData() {
        jdbc.update("delete from audit.audit_logs where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.stock_movements where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.payments where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoice_line_batches where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoice_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from sales.invoices where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.stocktake_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.stocktakes where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.inventory_balances where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.product_batches where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.goods_receipt_lines where organization_id=?", ORG_ID);
        jdbc.update("delete from inventory.goods_receipts where organization_id=?", ORG_ID);
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
            jdbc.update("delete from core.stores where id=?", OTHER_STORE_ID);
        } finally {
            if (POSTGRES.isRunning()) {
                POSTGRES.stop();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Cross-Module Regression: Receipt → Stocktake Sync/Approve → Sale with Promo → Reports
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void fullRegression_receive_stocktakeSyncAndApprove_quoteCheckoutWithPromo_ledgerAndReportsReconcile() {
        // Step 1: Receive goods (INV-01)
        // Apple 10.000 KG (delivered 10.000, accepted 10.000, cost 20,000, expiry +30d)
        // Rice 20.000 EA (delivered 20.000, accepted 20.000, cost 15,000, no expiry)
        UUID appleReceiptId = receive(APPLE_ID, "10.000", "10.000", "0.000", 20000, LocalDate.now(VN_ZONE).plusDays(30), null);
        UUID riceReceiptId = receive(RICE_ID, "20.000", "20.000", "0.000", 15000, null, null);

        assertThat(count("select count(*) from inventory.goods_receipts where id in (?, ?)", appleReceiptId, riceReceiptId)).isEqualTo(2);
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("10.000");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("20.000");
        assertThat(ledgerSum(APPLE_ID)).isEqualByComparingTo("10.000");
        assertThat(ledgerSum(RICE_ID)).isEqualByComparingTo("20.000");

        // Step 2: Stocktake Sync (SYN-02) and Approval (INV-03)
        // Query batch ID and version for Apple
        Map<String, Object> appleBatchInfo = jdbc.queryForMap("""
                select pb.id as batch_id, ib.version
                from inventory.product_batches pb
                join inventory.inventory_balances ib on ib.batch_id = pb.id
                where pb.organization_id=? and pb.store_id=? and pb.product_id=?
                limit 1
                """, ORG_ID, STORE_ID, APPLE_ID);
        UUID appleBatchId = (UUID) appleBatchInfo.get("batch_id");
        Long appleVersion = ((Number) appleBatchInfo.get("version")).longValue();

        // Stock user opens stocktake session
        ResponseEntity<Object> openStocktakeResp = post("/api/v1/inventory/stocktakes", Map.of(), stockToken);
        assertThat(openStocktakeResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID stocktakeId = UUID.fromString(body(openStocktakeResp).get("id").toString());

        // Stock user submits count: actual 9.500 KG (discrepancy -0.500 KG)
        UUID countOpId = UUID.randomUUID();
        Map<String, Object> countReq = Map.of(
                "clientOperationId", countOpId,
                "batchId", appleBatchId,
                "actualQuantity", "9.500",
                "baseVersion", appleVersion,
                "note", "Kiểm kê lệch 0.5kg táo dập");
        ResponseEntity<Object> submitCountResp = post(
                "/api/v1/inventory/stocktakes/" + stocktakeId + "/counts", countReq, stockToken);
        assertThat(submitCountResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(submitCountResp)).containsEntry("status", "PENDING");

        // Manager approves stocktake (INV-03)
        ResponseEntity<Object> approveResp = post(
                "/api/v1/inventory/stocktakes/" + stocktakeId + "/approve", Map.of(), managerToken);
        assertThat(approveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(approveResp)).containsEntry("status", "APPROVED");

        // Verify stock movement STOCKTAKE_ADJUSTMENT and updated balance
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("9.500");
        assertThat(ledgerSum(APPLE_ID)).isEqualByComparingTo("9.500");
        Map<String, Object> stocktakeMovement = jdbc.queryForMap("""
                select movement_type, quantity_delta, reference_id, reference_type
                from inventory.stock_movements
                where reference_id=? and reference_type='STOCKTAKE'
                """, stocktakeId);
        assertThat(stocktakeMovement.get("movement_type")).isEqualTo("STOCKTAKE_ADJUSTMENT");
        assertThat(decimal(stocktakeMovement.get("quantity_delta"))).isEqualByComparingTo("-0.500");

        // Step 3: Sales Promotion & Quote (PRO-01B, SAL-01)
        // Activate 10% discount promo on Rice
        jdbc.update("update sales.promotions set status='ACTIVE' where id=?", PROMO_RICE_ID);

        List<Map<String, Object>> cartItems = List.of(
                Map.of("productId", APPLE_ID, "quantity", "2.500"),
                Map.of("productId", RICE_ID, "quantity", "2.000"));

        Map<String, Object> quote = body(post("/api/v1/sales/quote", Map.of("items", cartItems), salesToken));
        // Apple: 2.5 * 40,000 = 100,000; Rice: 2 * 25,000 = 50,000 (10% promo -> discount 5,000)
        // Subtotal = 150,000; Discount = 5,000; GrandTotal = 145,000
        assertThat(quote.get("subtotal")).isEqualTo(150000);
        assertThat(quote.get("discountTotal")).isEqualTo(5000);
        assertThat(quote.get("grandTotal")).isEqualTo(145000);

        // Step 4: CASH Checkout (SAL-01)
        ResponseEntity<Object> checkoutResp = post("/api/v1/sales/checkout", Map.of(
                "items", cartItems,
                "cashAmount", 150000), salesToken);
        assertThat(checkoutResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> invoice = body(checkoutResp);
        UUID invoiceId = UUID.fromString(invoice.get("id").toString());
        assertThat(invoice)
                .containsEntry("grandTotal", 145000)
                .containsEntry("changeAmount", 5000);

        // Verify stored invoice & payment
        Map<String, Object> storedInvoice = jdbc.queryForMap("""
                select status, subtotal, discount_total, grand_total, paid_total, change_amount
                from sales.invoices where id=?
                """, invoiceId);
        assertThat(storedInvoice)
                .containsEntry("status", "COMPLETED")
                .containsEntry("subtotal", 150000L)
                .containsEntry("discount_total", 5000L)
                .containsEntry("grand_total", 145000L)
                .containsEntry("paid_total", 145000L)
                .containsEntry("change_amount", 5000L);

        assertThat(count("select count(*) from sales.payments where invoice_id=?", invoiceId)).isEqualTo(1);
        assertThat(count("select count(*) from sales.invoice_lines where invoice_id=?", invoiceId)).isEqualTo(2);
        assertThat(count("select count(*) from sales.invoice_line_batches ilb join sales.invoice_lines il on il.id=ilb.invoice_line_id where il.invoice_id=?", invoiceId)).isEqualTo(2);

        // Verify post-sale balances and ledger invariants
        // Apple: 9.500 - 2.500 = 7.000 KG
        // Rice: 20.000 - 2.000 = 18.000 EA
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo("7.000");
        assertThat(ledgerSum(APPLE_ID)).isEqualByComparingTo("7.000");
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo("18.000");
        assertThat(ledgerSum(RICE_ID)).isEqualByComparingTo("18.000");

        // Step 5: Reports Reconciliation (REP-01)
        LocalDate today = jdbc.queryForObject(
                        "select sold_at from sales.invoices where id=?",
                        OffsetDateTime.class, invoiceId)
                .atZoneSameInstant(VN_ZONE).toLocalDate();

        Map<String, Object> revenueReport = body(get("/api/v1/reports/revenue?from=" + today + "&to=" + today.plusDays(1), managerToken));
        Long sqlRevenue = jdbc.queryForObject("""
                select coalesce(sum(grand_total), 0) from sales.invoices
                where organization_id=? and store_id=? and status='COMPLETED'
                """, Long.class, ORG_ID, STORE_ID);
        Integer sqlInvoiceCount = jdbc.queryForObject("""
                select count(*) from sales.invoices
                where organization_id=? and store_id=? and status='COMPLETED'
                """, Integer.class, ORG_ID, STORE_ID);
        assertThat(decimal(revenueReport.get("revenue"))).isEqualByComparingTo(sqlRevenue.toString());
        assertThat(decimal(revenueReport.get("invoiceCount"))).isEqualByComparingTo(sqlInvoiceCount.toString());

        Map<String, Object> inventoryReport = body(get("/api/v1/reports/inventory", managerToken));
        List<Map<String, Object>> repItems = (List<Map<String, Object>>) inventoryReport.get("items");
        Map<String, Object> repApple = repItems.stream()
                .filter(i -> APPLE_ID.toString().equals(i.get("productId"))).findFirst().orElseThrow();
        assertThat(decimal(repApple.get("quantity"))).isEqualByComparingTo("7.000");
        Map<String, Object> repRice = repItems.stream()
                .filter(i -> RICE_ID.toString().equals(i.get("productId"))).findFirst().orElseThrow();
        assertThat(decimal(repRice.get("quantity"))).isEqualByComparingTo("18.000");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Security RBAC Matrix: 4 Roles + Anonymous 401 + Deny-by-default + Masking
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void securityRbacMatrix_allFourRolesEnforcePermissionsAndDenyByDefault() {
        // 1. Anonymous (no token) -> 401 Unauthorized
        assertThat(get("/api/v1/auth/session", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/inventory/products", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/reports/revenue?from=2026-10-01&to=2026-10-02", null).getStatusCode().value()).isEqualTo(401);
        assertThat(post("/api/v1/sales/quote", Map.of("items", List.of()), null).getStatusCode().value()).isEqualTo(401);
        assertThat(post("/api/v1/sales/checkout", Map.of("items", List.of()), null).getStatusCode().value()).isEqualTo(401);
        assertThat(post("/api/v1/inventory/stocktakes", Map.of(), null).getStatusCode().value()).isEqualTo(401);

        // 2. SALES role:
        // Allowed: quote (sales.read), session, inventory.read (without cost)
        assertThat(get("/api/v1/auth/session", salesToken).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Object> salesInvResp = get("/api/v1/inventory/products", salesToken);
        assertThat(salesInvResp.getStatusCode().value()).isEqualTo(200);
        assertThat(String.valueOf(salesInvResp.getBody()))
                .as("Sensitive purchase/unit cost fields must be masked for SALES")
                .doesNotContain("unitCost")
                .doesNotContain("unit_cost")
                .doesNotContain("purchase");

        // Denied for SALES (403):
        assertThat(post("/api/v1/inventory/receipts", Map.of(), salesToken).getStatusCode().value())
                .as("SALES cannot confirm goods receipt").isEqualTo(403);
        assertThat(post("/api/v1/inventory/stocktakes", Map.of(), salesToken).getStatusCode().value())
                .as("SALES cannot open stocktake session").isEqualTo(403);
        assertThat(post("/api/v1/inventory/stocktakes/" + UUID.randomUUID() + "/approve", Map.of(), salesToken).getStatusCode().value())
                .as("SALES cannot approve stocktake").isEqualTo(403);
        assertThat(get("/api/v1/reports/revenue?from=2026-10-01&to=2026-10-02", salesToken).getStatusCode().value())
                .as("SALES cannot access revenue reports").isEqualTo(403);
        assertThat(get("/api/v1/reports/inventory", salesToken).getStatusCode().value())
                .as("SALES cannot access inventory reports").isEqualTo(403);
        assertThat(post("/api/v1/promotions", Map.of(), salesToken).getStatusCode().value())
                .as("SALES cannot create promotions").isEqualTo(403);

        // 3. STOCK role:
        // Allowed: receipts, stocktake write
        assertThat(post("/api/v1/inventory/stocktakes", Map.of(), stockToken).getStatusCode().value()).isEqualTo(201);

        // Denied for STOCK (403):
        assertThat(post("/api/v1/sales/checkout", Map.of("items", List.of(), "cashAmount", 1000), stockToken).getStatusCode().value())
                .as("STOCK cannot perform sales checkout").isEqualTo(403);
        assertThat(post("/api/v1/inventory/stocktakes/" + UUID.randomUUID() + "/approve", Map.of(), stockToken).getStatusCode().value())
                .as("STOCK cannot approve stocktake").isEqualTo(403);
        assertThat(get("/api/v1/reports/revenue?from=2026-10-01&to=2026-10-02", stockToken).getStatusCode().value())
                .as("STOCK cannot access revenue reports").isEqualTo(403);

        // 4. MANAGER role:
        // Allowed: inventory read/write, stocktake approve, reports, checkout, promotions
        assertThat(get("/api/v1/reports/inventory", managerToken).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/reports/revenue?from=2026-10-01&to=2026-10-02", managerToken).getStatusCode().value()).isEqualTo(200);

        // 5. ADMIN role:
        // Allowed: administrative operations within store
        assertThat(get("/api/v1/reports/inventory", adminToken).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/reports/revenue?from=2026-10-01&to=2026-10-02", adminToken).getStatusCode().value()).isEqualTo(200);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Store Scope Isolation: Cross-store requests must be rejected
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void storeScopeIsolation_crossStoreOperationsAreRejected() {
        // Attempting to quote with a storeId different from session
        ResponseEntity<Object> quoteCrossStore = post("/api/v1/sales/quote", Map.of(
                "storeId", OTHER_STORE_ID,
                "items", List.of(Map.of("productId", APPLE_ID, "quantity", "1.000"))), salesToken);
        assertThat(quoteCrossStore.getStatusCode().value())
                .as("Store mismatch must be rejected")
                .isIn(400, 403, 422);

        // Attempting to checkout with a storeId different from session
        ResponseEntity<Object> checkoutCrossStore = post("/api/v1/sales/checkout", Map.of(
                "storeId", OTHER_STORE_ID,
                "items", List.of(Map.of("productId", APPLE_ID, "quantity", "1.000")),
                "cashAmount", 50000), salesToken);
        assertThat(checkoutCrossStore.getStatusCode().value())
                .as("Store mismatch must be rejected")
                .isIn(400, 403, 422);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. Invoice Privacy: Documents actual S2-GAP-03 behavior for SALES in same store
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void invoicePrivacy_documentsSalesVisibilityBehavior() {
        receive(RICE_ID, "5.000", "5.000", "0.000", 15000, null, null);

        // Sales1 creates an invoice
        ResponseEntity<Object> checkout = post("/api/v1/sales/checkout", Map.of(
                "items", List.of(Map.of("productId", RICE_ID, "quantity", "1.000")),
                "cashAmount", 30000), salesToken);
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID invoiceId = UUID.fromString(body(checkout).get("id").toString());

        // Sales2 lists invoices in the same store
        ResponseEntity<Object> listResp = get("/api/v1/sales/invoices", salesToken2);
        assertThat(listResp.getStatusCode().value()).isEqualTo(200);

        List<Map<String, Object>> invoices = (List<Map<String, Object>>) listResp.getBody();
        boolean seenBySales2 = invoices.stream().anyMatch(inv -> invoiceId.toString().equals(inv.get("id")));

        // In the current baseline, SaleController#list returns all store invoices without filtering by soldBy.
        // As documented in #34 (S2-GAP-03), SALES can see invoices created by other sellers in the same store.
        // We assert true here to document the real, verified status (FAIL for the requirement "SALES only sees own invoices").
        assertThat(seenBySales2)
                .as("S2-GAP-03: Current implementation allows Sales2 to see Sales1 invoices within same store (Fail against requirement)")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. Training Isolation (NFR-TRN-01): Session carries training flag, no operational writes
    //    NOTE: Ca cách ly end-to-end (thực hiện hành động đào tạo thật và đối soát bảng vận hành)
    //    hiện ở trạng thái Blocked do TRN-03 (API đào tạo backend) chưa được tích hợp lên main.
    //    Test dưới đây xác minh session mang cờ trainingEnabled và việc đọc session không làm
    //    phát sinh dữ liệu vào bảng vận hành. Ca E2E sẽ được kiểm chứng lại sau khi TRN-03 lên main.
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void trainingIsolation_sessionCarriesFlagAndOperationalTablesRemainClean() {
        long invoicesBefore = count("select count(*) from sales.invoices", new Object[0]);
        long movementsBefore = count("select count(*) from inventory.stock_movements", new Object[0]);

        // Training user session carries trainingEnabled=true
        ResponseEntity<Object> sessionResp = get("/api/v1/auth/session", trainingUserToken);
        assertThat(sessionResp.getStatusCode().value()).isEqualTo(200);
        assertThat(body(sessionResp)).containsEntry("trainingEnabled", true);

        // Regular user session carries trainingEnabled=false
        ResponseEntity<Object> normalSessionResp = get("/api/v1/auth/session", salesToken);
        assertThat(normalSessionResp.getStatusCode().value()).isEqualTo(200);
        assertThat(body(normalSessionResp)).containsEntry("trainingEnabled", false);

        // Operational tables remain completely unchanged
        assertThat(count("select count(*) from sales.invoices", new Object[0])).isEqualTo(invoicesBefore);
        assertThat(count("select count(*) from inventory.stock_movements", new Object[0])).isEqualTo(movementsBefore);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. Rollback & Concurrency Safety
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void rollbackSafety_insufficientStockRollsBackEntireCart() {
        receive(RICE_ID, "1.000", "1.000", "0.000", 15000, null, null);
        receive(APPLE_ID, "1.000", "1.000", "0.000", 20000, LocalDate.now(VN_ZONE).plusDays(30), null);

        BigDecimal riceBefore = productBalance(RICE_ID);
        BigDecimal appleBefore = productBalance(APPLE_ID);

        // Cart requesting 1 EA rice (sufficient) and 2.000 KG apple (insufficient, only 1.000 available)
        ResponseEntity<Object> resp = post("/api/v1/sales/checkout", Map.of(
                "items", List.of(
                        Map.of("productId", RICE_ID, "quantity", "1.000"),
                        Map.of("productId", APPLE_ID, "quantity", "2.000")),
                "cashAmount", 200000), salesToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(productBalance(RICE_ID)).isEqualByComparingTo(riceBefore);
        assertThat(productBalance(APPLE_ID)).isEqualByComparingTo(appleBefore);
        assertThat(count("select count(*) from sales.invoices", new Object[0])).isZero();
        assertThat(count("select count(*) from sales.payments", new Object[0])).isZero();
        assertThat(count("select count(*) from inventory.stock_movements where movement_type='SALE'", new Object[0])).isZero();
    }

    @Test
    void concurrency_lastUnitRaceProducesOne201AndOne409() throws Exception {
        receive(APPLE_ID, "1.000", "1.000", "0.000", 20000, LocalDate.now(VN_ZONE).plusDays(30), null);
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

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. Backup and Restore on Clean PostgreSQL Environment (NFR-BAC-01)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void backupAndRestore_cleanPostgresRestoresAllEntitiesAndPreservesInvariants() throws Exception {
        // Step 1: Create business transactions to be backed up
        receive(APPLE_ID, "5.000", "5.000", "0.000", 20000, LocalDate.now(VN_ZONE).plusDays(30), null);
        receive(RICE_ID, "10.000", "10.000", "0.000", 15000, null, null);

        // Perform a sale
        post("/api/v1/sales/checkout", Map.of(
                "items", List.of(
                        Map.of("productId", APPLE_ID, "quantity", "1.500"),
                        Map.of("productId", RICE_ID, "quantity", "2.000")),
                "cashAmount", 120000), salesToken);

        // Step 2: Record pre-backup entity counts and revenue oracle
        int preUsers = count("select count(*) from iam.users", new Object[0]);
        int preRoles = count("select count(*) from iam.roles", new Object[0]);
        int preCategories = count("select count(*) from catalog.categories", new Object[0]);
        int preProducts = count("select count(*) from catalog.products", new Object[0]);
        int preReceipts = count("select count(*) from inventory.goods_receipts", new Object[0]);
        int preBatches = count("select count(*) from inventory.product_batches", new Object[0]);
        int preBalances = count("select count(*) from inventory.inventory_balances", new Object[0]);
        int preMovements = count("select count(*) from inventory.stock_movements", new Object[0]);
        int preInvoices = count("select count(*) from sales.invoices", new Object[0]);
        int prePayments = count("select count(*) from sales.payments", new Object[0]);
        Long preRevenue = jdbc.queryForObject(
                "select coalesce(sum(grand_total), 0) from sales.invoices where status='COMPLETED'", Long.class);

        BigDecimal preAppleOnHand = productBalance(APPLE_ID);
        BigDecimal preRiceOnHand = productBalance(RICE_ID);

        // Step 3: Run pg_dump inside container to /tmp/backup_qa04.sql
        String dumpPath = "/tmp/backup_qa04.sql";
        Container.ExecResult dumpResult = POSTGRES.execInContainer(
                "pg_dump", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-Fp", "-f", dumpPath);
        assertThat(dumpResult.getExitCode())
                .as("pg_dump must exit with code 0: %s", dumpResult.getStderr())
                .isZero();

        Container.ExecResult checkFile = POSTGRES.execInContainer("ls", "-la", dumpPath);
        assertThat(checkFile.getExitCode()).isZero();
        assertThat(checkFile.getStdout()).contains("backup_qa04.sql");

        // Step 4: Create a clean database 'simtim_clean_restore' inside the PostgreSQL server
        String cleanDb = "simtim_clean_restore";
        POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "DROP DATABASE IF EXISTS " + cleanDb + ";");
        Container.ExecResult createDbResult = POSTGRES.execInContainer(
                "psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "CREATE DATABASE " + cleanDb + ";");
        assertThat(createDbResult.getExitCode()).isZero();

        // Step 5: Restore the dump into the clean database
        Container.ExecResult restoreResult = POSTGRES.execInContainer(
                "psql", "-v", "ON_ERROR_STOP=1", "-U", POSTGRES.getUsername(), "-d", cleanDb, "-f", dumpPath);
        assertThat(restoreResult.getExitCode())
                .as("psql restore into clean DB must exit with code 0: %s", restoreResult.getStderr())
                .isZero();

        // Step 6: Verify restored database using a separate JdbcTemplate connected to 'simtim_clean_restore'
        String cleanJdbcUrl = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + cleanDb);
        DriverManagerDataSource cleanDs = new DriverManagerDataSource(
                cleanJdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
        cleanDs.setDriverClassName(POSTGRES.getDriverClassName());
        JdbcTemplate cleanJdbc = new JdbcTemplate(cleanDs);

        // Assert 100% exact match of all entity counts
        assertThat(cleanJdbc.queryForObject("select count(*) from iam.users", Integer.class)).isEqualTo(preUsers);
        assertThat(cleanJdbc.queryForObject("select count(*) from iam.roles", Integer.class)).isEqualTo(preRoles);
        assertThat(cleanJdbc.queryForObject("select count(*) from catalog.categories", Integer.class)).isEqualTo(preCategories);
        assertThat(cleanJdbc.queryForObject("select count(*) from catalog.products", Integer.class)).isEqualTo(preProducts);
        assertThat(cleanJdbc.queryForObject("select count(*) from inventory.goods_receipts", Integer.class)).isEqualTo(preReceipts);
        assertThat(cleanJdbc.queryForObject("select count(*) from inventory.product_batches", Integer.class)).isEqualTo(preBatches);
        assertThat(cleanJdbc.queryForObject("select count(*) from inventory.inventory_balances", Integer.class)).isEqualTo(preBalances);
        assertThat(cleanJdbc.queryForObject("select count(*) from inventory.stock_movements", Integer.class)).isEqualTo(preMovements);
        assertThat(cleanJdbc.queryForObject("select count(*) from sales.invoices", Integer.class)).isEqualTo(preInvoices);
        assertThat(cleanJdbc.queryForObject("select count(*) from sales.payments", Integer.class)).isEqualTo(prePayments);

        // Assert financial oracle match
        Long restoredRevenue = cleanJdbc.queryForObject(
                "select coalesce(sum(grand_total), 0) from sales.invoices where status='COMPLETED'", Long.class);
        assertThat(restoredRevenue).isEqualTo(preRevenue);

        // Assert inventory balances match
        BigDecimal restoredApple = cleanJdbc.queryForObject("""
                select coalesce(sum(on_hand_quantity), 0) from inventory.inventory_balances
                where organization_id=? and store_id=? and product_id=?
                """, BigDecimal.class, ORG_ID, STORE_ID, APPLE_ID);
        BigDecimal restoredRice = cleanJdbc.queryForObject("""
                select coalesce(sum(on_hand_quantity), 0) from inventory.inventory_balances
                where organization_id=? and store_id=? and product_id=?
                """, BigDecimal.class, ORG_ID, STORE_ID, RICE_ID);
        assertThat(restoredApple).isEqualByComparingTo(preAppleOnHand);
        assertThat(restoredRice).isEqualByComparingTo(preRiceOnHand);

        // Assert ledger invariant in restored database: on_hand_quantity = SUM(quantity_delta)
        List<Map<String, Object>> batches = cleanJdbc.queryForList("""
                select ib.batch_id, ib.on_hand_quantity,
                       coalesce(sum(sm.quantity_delta), 0) as ledger_quantity
                from inventory.inventory_balances ib
                left join inventory.stock_movements sm
                  on sm.organization_id=ib.organization_id and sm.store_id=ib.store_id
                 and sm.product_id=ib.product_id and sm.batch_id=ib.batch_id
                where ib.organization_id=? and ib.store_id=?
                group by ib.batch_id, ib.on_hand_quantity
                """, ORG_ID, STORE_ID);
        assertThat(batches).isNotEmpty();
        for (Map<String, Object> batch : batches) {
            BigDecimal onHand = decimal(batch.get("on_hand_quantity"));
            BigDecimal ledger = decimal(batch.get("ledger_quantity"));
            assertThat(onHand)
                    .as("Invariant must hold post-restore for batch %s", batch.get("batch_id"))
                    .isEqualByComparingTo(ledger);
        }

        // Teardown clean DB
        POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "DROP DATABASE " + cleanDb + ";");
    }

    @Test
    void restore_corruptedDump_failsWhenOnErrorStopEnabled() throws Exception {
        String testDb = "simtim_corrupted_restore_test";
        POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "DROP DATABASE IF EXISTS " + testDb + ";");
        Container.ExecResult createDbResult = POSTGRES.execInContainer(
                "psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "CREATE DATABASE " + testDb + ";");
        assertThat(createDbResult.getExitCode()).isZero();

        String corruptedDumpPath = "/tmp/corrupted_dump.sql";
        POSTGRES.execInContainer("sh", "-c",
                "echo 'CREATE TABLE test_table (id int); MALFORMED SQL SYNTAX ERROR;' > " + corruptedDumpPath);

        // With -v ON_ERROR_STOP=1, psql must abort and return non-zero exit code on SQL error
        Container.ExecResult failResult = POSTGRES.execInContainer(
                "psql", "-v", "ON_ERROR_STOP=1", "-U", POSTGRES.getUsername(), "-d", testDb, "-f", corruptedDumpPath);
        assertThat(failResult.getExitCode())
                .as("psql must exit with non-zero code when ON_ERROR_STOP=1 encounters malformed SQL: %s",
                        failResult.getStderr())
                .isNotZero();

        // Teardown
        POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "postgres",
                "-c", "DROP DATABASE " + testDb + ";");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private UUID receive(UUID productId, String delivered, String accepted, String rejected,
                          long unitCost, LocalDate expiryDate, String reason) {
        UUID clientOperationId = UUID.randomUUID();
        var line = new LinkedHashMap<String, Object>();
        line.put("productId", productId);
        line.put("expectedQuantity", delivered);
        line.put("deliveredQuantity", delivered);
        line.put("acceptedQuantity", accepted);
        line.put("rejectedQuantity", rejected);
        line.put("unitCost", unitCost);
        if (expiryDate != null) {
            line.put("expiryDate", expiryDate.toString());
        }
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

    private String createActor(String role, boolean trainingEnabled) {
        UUID userId = UUID.randomUUID();
        fixtureUsers.add(userId);
        String username = "qa04_" + role.toLowerCase(Locale.ROOT) + "_" + userId;
        jdbc.update("""
                insert into iam.users(id, organization_id, username, password_hash, full_name, status, training_enabled)
                values(?, ?, ?, ?, ?, 'ACTIVE', ?)
                """, userId, ORG_ID, username, passwords.hash(PASSWORD), "QA-04 " + role, trainingEnabled);
        assertThat(jdbc.update("""
                insert into iam.user_roles(organization_id, user_id, role_id, store_id, assigned_at, assigned_by)
                select organization_id, ?, id, ?, now(), ? from iam.roles
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
                select coalesce(sum(on_hand_quantity), 0)
                from inventory.inventory_balances
                where organization_id=? and store_id=? and product_id=?
                """, BigDecimal.class, ORG_ID, STORE_ID, productId);
    }

    private BigDecimal ledgerSum(UUID productId) {
        return jdbc.queryForObject("""
                select coalesce(sum(quantity_delta), 0)
                from inventory.stock_movements
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
