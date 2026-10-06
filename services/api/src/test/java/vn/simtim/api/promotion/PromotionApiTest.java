package vn.simtim.api.promotion;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Integration test cho PRO-01B: khuyến mãi cơ bản.
 * Chạy trên PostgreSQL 17 (Testcontainers) với profile demo.
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PromotionApiTest {

    static final String ORG_ID    = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID  = "10000000-0000-0000-0000-000000000002";
    static final String PRODUCT_RICE_ID  = "10000000-0000-0000-0000-000000000041";
    static final String PRODUCT_APPLE_ID = "10000000-0000-0000-0000-000000000042";
    static final String BATCH_RICE_ID    = "10000000-0000-0000-0000-0000000000a1";
    static final String DEMO_PROMO_ID    = "10000000-0000-0000-0000-000000000081";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_promotion_test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        String externalUrl = System.getenv("SIMTIM_TEST_DB_URL");
        if (externalUrl == null || externalUrl.isBlank()) {
            postgres.start();
            r.add("spring.datasource.url", postgres::getJdbcUrl);
            r.add("spring.datasource.username", postgres::getUsername);
            r.add("spring.datasource.password", postgres::getPassword);
        } else {
            r.add("spring.datasource.url", () -> externalUrl);
            r.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            r.add("spring.datasource.password", () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;

    private final List<UUID> fixtureUsers = new ArrayList<>();
    private final List<UUID> fixtureStores = new ArrayList<>();
    private static final String PASSWORD = "promo-fixture-password-123";
    private String managerToken;
    private String salesToken;
    /** Manager thuộc store thứ hai (cùng org) dùng để kiểm tra cross-store isolation. */
    private String otherStoreManagerToken;

    @BeforeAll
    void authenticateFixtures() {
        String hash = passwords.hash(PASSWORD);
        managerToken         = createSession("MANAGER", hash, UUID.fromString(STORE_ID));
        salesToken           = createSession("SALES",   hash, UUID.fromString(STORE_ID));
        // Tạo store thứ 2 cùng org cho cross-store tests
        UUID store2Id = UUID.randomUUID();
        fixtureStores.add(store2Id);
        jdbc.update("insert into core.stores(id,organization_id,code,name,status) values(?,?,'STORE2','Cửa hàng 2','ACTIVE')",
                store2Id, UUID.fromString(ORG_ID));
        // Tạo role MANAGER trong store2 (dùng lại role id của org)
        otherStoreManagerToken = createSession("MANAGER", hash, store2Id);
        seedBatchIfAbsent();
    }

    @AfterAll
    void stop() {
        try {
            jdbc.update("delete from sales.promotion_batches where organization_id=?", UUID.fromString(ORG_ID));
            jdbc.update("delete from sales.promotion_products where organization_id=? and promotion_id <> ?",
                    UUID.fromString(ORG_ID), UUID.fromString(DEMO_PROMO_ID));
            jdbc.update("delete from sales.promotions where organization_id=? and id <> ?",
                    UUID.fromString(ORG_ID), UUID.fromString(DEMO_PROMO_ID));
            jdbc.update("delete from inventory.product_batches where organization_id=?", UUID.fromString(ORG_ID));
            jdbc.update("delete from inventory.goods_receipt_lines where organization_id=?", UUID.fromString(ORG_ID));
            jdbc.update("delete from inventory.goods_receipts where organization_id=?", UUID.fromString(ORG_ID));
            for (UUID id : fixtureUsers) {
                jdbc.update("delete from iam.auth_sessions where user_id=?", id);
                jdbc.update("delete from iam.user_roles where user_id=?", id);
                jdbc.update("delete from iam.users where id=?", id);
            }
            for (UUID id : fixtureStores) {
                jdbc.update("delete from core.stores where id=?", id);
            }
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    private void seedBatchIfAbsent() {
        UUID org = UUID.fromString(ORG_ID);
        UUID store = UUID.fromString(STORE_ID);
        UUID supplierId = UUID.fromString("10000000-0000-0000-0000-000000000061");
        UUID receiptId = UUID.fromString("10000000-0000-0000-0000-000000000081");
        UUID lineRiceId = UUID.fromString("10000000-0000-0000-0000-000000000091");
        UUID batchRiceId = UUID.fromString(BATCH_RICE_ID);
        UUID productRice = UUID.fromString(PRODUCT_RICE_ID);
        UUID user = fixtureUsers.get(0);

        jdbc.update("""
                insert into inventory.goods_receipts
                    (id,organization_id,store_id,supplier_id,status,received_at,confirmed_by,
                     client_operation_id,idempotency_key,payload_hash)
                values(?,?,?,?,'CONFIRMED','2026-01-01T00:00:00Z',?,?,?,'\\\\x00'::bytea)
                on conflict do nothing
                """, receiptId, org, store, supplierId, user, UUID.randomUUID(), UUID.randomUUID());

        jdbc.update("""
                insert into inventory.goods_receipt_lines
                    (id,receipt_id,organization_id,product_id,
                     expected_quantity,delivered_quantity,accepted_quantity,rejected_quantity,unit_cost)
                values(?,?,?,?,100,100,100,0,18000)
                on conflict do nothing
                """, lineRiceId, receiptId, org, productRice);

        jdbc.update("""
                insert into inventory.product_batches
                    (id,organization_id,store_id,product_id,receipt_line_id,batch_number,
                     received_date,expiry_date,status)
                values(?,?,?,?,?,'BATCH-RICE-01','2026-01-01','2027-01-01','AVAILABLE')
                on conflict do nothing
                """, batchRiceId, org, store, productRice, lineRiceId);
    }

    private String createSession(String role, String hash, UUID storeId) {
        UUID id = UUID.randomUUID();
        String username = "promo_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                values(?,?,?,?,?,'ACTIVE')
                """, id, UUID.fromString(ORG_ID), username, hash, "Promo fixture " + role);
        fixtureUsers.add(id);
        assertThat(jdbc.update("""
                insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                select organization_id,?,id,?,now(),? from iam.roles
                where organization_id=? and code=?
                """, id, storeId, id, UUID.fromString(ORG_ID), role)).isEqualTo(1);
        // Đăng nhập: tìm storeCode
        String storeCode = jdbc.queryForObject(
                "select code from core.stores where id=?", String.class, storeId);
        var response = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of(
                "organizationCode", "SIMTIM", "storeCode", storeCode,
                "username", username, "password", PASSWORD), scopeHeaders()), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders scopeHeaders() {
        var h = new HttpHeaders();
        h.set("X-Organization-Id", ORG_ID);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders managerHeaders() {
        var h = scopeHeaders();
        h.setBearerAuth(managerToken);
        return h;
    }

    private HttpHeaders salesHeaders() {
        var h = scopeHeaders();
        h.setBearerAuth(salesToken);
        return h;
    }

    /** Header với org-id khác để kiểm tra 403 cross-org scope. */
    private HttpHeaders wrongOrgHeaders() {
        var h = new HttpHeaders();
        h.set("X-Organization-Id", UUID.randomUUID().toString());
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(managerToken);
        return h;
    }

    /** Header của manager ở store thứ 2 (cùng org). */
    private HttpHeaders otherStoreHeaders() {
        var h = scopeHeaders();
        h.setBearerAuth(otherStoreManagerToken);
        return h;
    }

    // ===== Bảo mật =====

    @Test
    void promotionRequestWithoutBearerIsRejected() {
        var res = rest.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(scopeHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(401);
        assertThat(res.getBody()).containsEntry("code", "UNAUTHENTICATED");
    }

    @Test
    void list_withWrongOrg_returns403() {
        var res = rest.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(wrongOrgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void applicable_withWrongOrg_returns403() {
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_RICE_ID
                + "&at=2026-10-15T12:00:00Z";
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(wrongOrgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    /**
     * Xác nhận applicable dùng storeId từ principal (cửa hàng MAIN của session),
     * không cần client cung cấp storeId qua query param.
     */
    @Test
    void applicable_usesStoreFromPrincipal_notQueryParam() {
        // Không truyền storeId → vẫn trả về kết quả (storeId lấy từ principal)
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_RICE_ID
                + "&at=2026-10-15T12:00:00Z";
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        // Demo promo áp dụng cho gạo tại MAIN
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void salesCanReadButCannotWrite() {
        // Đọc được
        var read = rest.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(read.getStatusCode().value()).isEqualTo(200);

        // Ghi bị từ chối
        String body = validPromoJson("PERM-TEST-01");
        var post = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, salesHeaders()), Map.class);
        assertThat(post.getStatusCode().value()).isEqualTo(403);
        assertThat(post.getBody()).containsEntry("code", "FORBIDDEN");
    }

    // ===== Demo data =====

    @Test
    void listPromotions_returnsDemoPromotion() {
        var res = rest.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void getDemoPromotion_returnsWithProductScope() {
        var res = rest.exchange("/api/v1/promotions/" + DEMO_PROMO_ID, HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).containsEntry("code", "PROMO-RICE-10PCT");
        var productIds = (List<?>) res.getBody().get("productIds");
        assertThat(productIds).hasSize(1);
        assertThat(productIds.get(0).toString()).isEqualToIgnoringCase(PRODUCT_RICE_ID);
    }

    // ===== CRUD =====

    @Test
    void createPromotion_then_conflict_on_duplicateCode() {
        String body = validPromoJson("PROMO-DUP-01");
        var first = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(first.getBody()).containsKey("id");

        // Tạo lại cùng mã → 409
        var dup = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(dup.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void createPromotion_invalidPayload_returns422() {
        // Thiếu trường bắt buộc
        var res = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>("{\"code\":\"\"}", managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void createPromotion_invalidTimeWindow_returns422() {
        String body = """
                {"code":"BAD-TIME","name":"Sai thời gian","discountType":"PERCENT","discountValue":10,
                 "startsAt":"2026-12-31T00:00:00Z","endsAt":"2026-01-01T00:00:00Z","status":"DRAFT"}""";
        var res = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void createPromotion_percentOver100_returns422() {
        String body = """
                {"code":"BAD-PCT","name":"Vượt 100%","discountType":"PERCENT","discountValue":150,
                 "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T00:00:00Z","status":"DRAFT"}""";
        var res = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void createPromotion_percentWith3DecimalPlaces_returns422() {
        // 12.345 có scale=3 > 2 → numeric(14,2) sẽ làm tròn thành 12.35 không như mong muốn
        String body = """
                {"code":"BAD-SCALE","name":"Scale quá 2","discountType":"PERCENT","discountValue":12.345,
                 "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T00:00:00Z","status":"DRAFT"}""";
        var res = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void createPromotion_percentWith2DecimalPlaces_succeeds() {
        // 10.50 có scale=2 → hợp lệ
        String body = """
                {"code":"GOOD-SCALE","name":"Scale đúng 2","discountType":"PERCENT","discountValue":10.50,
                 "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T00:00:00Z","status":"DRAFT"}""";
        var res = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void createAndUpdatePromotion_then_delete() {
        // Tạo
        var created = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson("PROMO-CUD-01"), managerHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();

        // Cập nhật
        String updateBody = """
                {"code":"PROMO-CUD-01","name":"Đã cập nhật","discountType":"AMOUNT","discountValue":5000,
                 "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T00:00:00Z","status":"INACTIVE"}""";
        var updated = rest.exchange("/api/v1/promotions/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, managerHeaders()), Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("status", "INACTIVE");
        assertThat(updated.getBody()).containsEntry("discountType", "AMOUNT");

        // Xóa
        var del = rest.exchange("/api/v1/promotions/" + id, HttpMethod.DELETE,
                new HttpEntity<>(managerHeaders()), Void.class);
        assertThat(del.getStatusCode().value()).isEqualTo(204);

        // Xác nhận đã xóa
        var gone = rest.exchange("/api/v1/promotions/" + id, HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Map.class);
        assertThat(gone.getStatusCode().value()).isEqualTo(404);
    }

    // ===== Phạm vi sản phẩm =====

    @Test
    void addAndRemoveProductScope() {
        // Tạo khuyến mãi
        var created = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson("PROMO-SCOPE-01"), managerHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();

        // Thêm sản phẩm
        var add = rest.exchange("/api/v1/promotions/" + id + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", UUID.fromString(PRODUCT_RICE_ID)), managerHeaders()),
                Void.class);
        assertThat(add.getStatusCode().value()).isEqualTo(201);

        // Thêm lại cùng sản phẩm → 409
        var dup = rest.exchange("/api/v1/promotions/" + id + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", UUID.fromString(PRODUCT_RICE_ID)), managerHeaders()),
                Map.class);
        assertThat(dup.getStatusCode().value()).isEqualTo(409);

        // Lấy danh sách sản phẩm
        var products = rest.exchange("/api/v1/promotions/" + id + "/products", HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Object[].class);
        assertThat(products.getStatusCode().value()).isEqualTo(200);
        assertThat(products.getBody()).hasSize(1);

        // Xóa sản phẩm khỏi phạm vi
        var remove = rest.exchange("/api/v1/promotions/" + id + "/products/" + PRODUCT_RICE_ID,
                HttpMethod.DELETE, new HttpEntity<>(managerHeaders()), Void.class);
        assertThat(remove.getStatusCode().value()).isEqualTo(204);

        // Kiểm tra danh sách trống
        var empty = rest.exchange("/api/v1/promotions/" + id + "/products", HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Object[].class);
        assertThat(empty.getStatusCode().value()).isEqualTo(200);
        assertThat(empty.getBody()).isEmpty();
    }

    // ===== Tra cứu điểm bán =====

    @Test
    void applicable_returnsDemoPromotion_forRiceInOctober() {
        // Thời điểm trong cửa sổ khuyến mãi demo (10/2026); storeId lấy từ principal
        String at = "2026-10-15T12:00:00Z";
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_RICE_ID + "&at=" + at;
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotEmpty();
        @SuppressWarnings("unchecked")
        var first = (Map<String, Object>) res.getBody()[0];
        assertThat(first.get("code").toString()).isEqualTo("PROMO-RICE-10PCT");
    }

    @Test
    void applicable_returnsEmpty_forAppleInOctober() {
        // Khuyến mãi demo chỉ áp dụng cho gạo, táo không được giảm
        String at = "2026-10-15T12:00:00Z";
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_APPLE_ID + "&at=" + at;
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        // Không có khuyến mãi nào phủ toàn sản phẩm nên kết quả trống
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void applicable_returnsEmpty_outsideTimeWindow() {
        // Ngày 1/9/2026 — trước cửa sổ khuyến mãi tháng 10
        String at = "2026-09-01T00:00:00Z";
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_RICE_ID + "&at=" + at;
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void universalPromotion_appliesTo_allProducts() {
        // Tạo khuyến mãi KHÔNG giới hạn sản phẩm (universal)
        String body = """
                {"code":"PROMO-UNIVERSAL-01","name":"Giảm toàn bộ","discountType":"PERCENT",
                 "discountValue":5,"startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T23:59:59Z",
                 "status":"ACTIVE"}""";
        var created = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(body, managerHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);

        // Táo phải được hưởng khuyến mãi universal
        String at = "2026-10-15T12:00:00Z";
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_APPLE_ID + "&at=" + at;
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotEmpty();
        @SuppressWarnings("unchecked")
        var codes = java.util.Arrays.stream(res.getBody())
                .map(o -> ((Map<String, Object>) o).get("code").toString())
                .toList();
        assertThat(codes).contains("PROMO-UNIVERSAL-01");
    }

    @Test
    void salesPromotionsPrefix_worksIdentically() {
        var res = rest.exchange("/api/v1/sales/promotions", HttpMethod.GET,
                new HttpEntity<>(salesHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
    }

    // ===== Cross-store isolation (cùng org, store khác) =====

    @Test
    void crossStore_list_returnsEmptyForOtherStore() {
        // Store 2 chưa có promotion nào → danh sách trống
        var res = rest.exchange("/api/v1/promotions", HttpMethod.GET,
                new HttpEntity<>(otherStoreHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void crossStore_get_returns404ForPromotionOfOtherStore() {
        // Demo promotion thuộc STORE MAIN; manager ở store2 không được thấy
        var res = rest.exchange("/api/v1/promotions/" + DEMO_PROMO_ID, HttpMethod.GET,
                new HttpEntity<>(otherStoreHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void crossStore_delete_returns404ForPromotionOfOtherStore() {
        // Tạo promotion ở store MAIN
        var created = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson("CS-DEL-01"), managerHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String id = created.getBody().get("id").toString();

        // Manager ở store2 cố xóa → 404
        var del = rest.exchange("/api/v1/promotions/" + id, HttpMethod.DELETE,
                new HttpEntity<>(otherStoreHeaders()), Map.class);
        assertThat(del.getStatusCode().value()).isEqualTo(404);

        // Manager store MAIN vẫn xóa được
        var ok = rest.exchange("/api/v1/promotions/" + id, HttpMethod.DELETE,
                new HttpEntity<>(managerHeaders()), Void.class);
        assertThat(ok.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void crossStore_applicable_returnsEmptyForOtherStore() {
        // Store2 chưa có promotion nào → applicable trả về danh sách trống
        String url = "/api/v1/promotions/applicable?productId=" + PRODUCT_RICE_ID
                + "&at=2026-10-15T12:00:00Z";
        var res = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(otherStoreHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        // Demo promo thuộc MAIN, không lộ sang store2
        assertThat(res.getBody()).isEmpty();
    }

    /**
     * Hai store trong cùng một org không được dùng trùng code khuyến mãi (kể cả case-insensitive).
     * Trước khi sửa lỗi, store B sẽ qua precheck rồi vấp UNIQUE constraint → 500.
     * Sau khi sửa, precheck kiểm tra toàn org → trả về 409 đúng cấu trúc.
     */
    @Test
    void crossStore_sameOrg_duplicateCode_returns409() {
        String code = "CROSS-CODE-01";
        // Store MAIN tạo trước
        var first = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), managerHeaders()), Map.class);
        assertThat(first.getStatusCode().value()).isEqualTo(201);

        // Store2 (cùng org) dùng cùng code → phải bị từ chối 409
        var dup = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), otherStoreHeaders()), Map.class);
        assertThat(dup.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void crossStore_sameOrg_duplicateCode_caseInsensitive_returns409() {
        String codeUpper = "CROSS-CODE-CASE-01";
        String codeLower = "cross-code-case-01";
        // Store MAIN tạo với chữ hoa
        var first = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(codeUpper), managerHeaders()), Map.class);
        assertThat(first.getStatusCode().value()).isEqualTo(201);

        // Store2 dùng chữ thường → vẫn trùng theo unique index lower(code)
        var dup = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(codeLower), otherStoreHeaders()), Map.class);
        assertThat(dup.getStatusCode().value()).isEqualTo(409);
    }

    // ===== Target BATCH scope tests =====

    @Test
    void batchScope_lifecycle_add_list_and_remove() {
        String code = "PROMO-BATCH-CRUD-01";
        var createRes = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), managerHeaders()), Map.class);
        assertThat(createRes.getStatusCode().value()).isEqualTo(201);
        String promoId = (String) createRes.getBody().get("id");

        // 1. Thêm batch target
        var addRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), managerHeaders()), Void.class);
        assertThat(addRes.getStatusCode().value()).isEqualTo(201);

        // 2. Liệt kê batches qua endpoint /batches
        var listRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), List.class);
        assertThat(listRes.getStatusCode().value()).isEqualTo(200);
        assertThat(listRes.getBody()).hasSize(1);

        // 3. Tra cứu chi tiết promotion chứa batchIds
        var getRes = rest.exchange("/api/v1/promotions/" + promoId, HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), Map.class);
        assertThat(getRes.getStatusCode().value()).isEqualTo(200);
        var batchIds = (List<?>) getRes.getBody().get("batchIds");
        assertThat(batchIds.toString()).contains(BATCH_RICE_ID);

        // 4. Xóa batch target
        var delRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches/" + BATCH_RICE_ID,
                HttpMethod.DELETE, new HttpEntity<>(managerHeaders()), Void.class);
        assertThat(delRes.getStatusCode().value()).isEqualTo(204);

        // 5. Kiểm tra danh sách sau xóa rỗng
        var listAfterDel = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.GET,
                new HttpEntity<>(managerHeaders()), List.class);
        assertThat(listAfterDel.getBody()).isEmpty();
    }

    @Test
    void batchScope_rejectsBatchFromAnotherStoreInSameOrganization() {
        var created = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson("PROMO-BATCH-STORE-02"), otherStoreHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String promotionId = (String) created.getBody().get("id");

        var rejected = rest.exchange("/api/v1/promotions/" + promotionId + "/batches",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), otherStoreHeaders()),
                Map.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(404);
        assertThat(jdbc.queryForObject(
                "select count(*) from sales.promotion_batches where promotion_id=?",
                Integer.class, UUID.fromString(promotionId))).isZero();
    }

    @Test
    void applicable_includesStartAndExcludesEndOfWindow() {
        var created = rest.exchange("/api/v1/sales/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson("PROMO-WINDOW-BOUNDARY").replace(
                        "\"status\":\"DRAFT\"", "\"status\":\"ACTIVE\""), managerHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String id = (String) created.getBody().get("id");
        var scope = rest.exchange("/api/v1/sales/promotions/" + id + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", PRODUCT_RICE_ID), managerHeaders()), Void.class);
        assertThat(scope.getStatusCode().value()).isEqualTo(201);

        try {
            for (String instant : List.of("2026-09-30T23:59:59Z", "2026-10-01T00:00:00Z",
                    "2026-10-30T23:59:59Z", "2026-10-31T00:00:00Z")) {
                var response = rest.exchange("/api/v1/sales/promotions/applicable?productId="
                                + PRODUCT_RICE_ID + "&at=" + instant,
                        HttpMethod.GET, new HttpEntity<>(managerHeaders()), List.class);
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                boolean present = response.getBody().stream().anyMatch(item ->
                        id.equals(((Map<?, ?>) item).get("id")));
                assertThat(present).as(instant).isEqualTo(
                        instant.equals("2026-10-01T00:00:00Z")
                                || instant.equals("2026-10-30T23:59:59Z"));
            }
        } finally {
            jdbc.update("UPDATE sales.promotions SET status='INACTIVE' WHERE id=?::uuid", id);
        }
    }

    @Test
    void cannotMix_batchAndProductTargets() {
        // Khuyến mãi có sẵn PRODUCT target -> không thể thêm BATCH
        String codeP = "PROMO-MIX-P-01";
        var createP = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(codeP), managerHeaders()), Map.class);
        String promoPId = (String) createP.getBody().get("id");
        rest.exchange("/api/v1/promotions/" + promoPId + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", PRODUCT_RICE_ID), managerHeaders()), Void.class);

        var addBatchFail = rest.exchange("/api/v1/promotions/" + promoPId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), managerHeaders()), Map.class);
        assertThat(addBatchFail.getStatusCode().value()).isEqualTo(422);

        // Khuyến mãi có sẵn BATCH target -> không thể thêm PRODUCT
        String codeB = "PROMO-MIX-B-01";
        var createB = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(codeB), managerHeaders()), Map.class);
        String promoBId = (String) createB.getBody().get("id");
        rest.exchange("/api/v1/promotions/" + promoBId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), managerHeaders()), Void.class);

        var addProductFail = rest.exchange("/api/v1/promotions/" + promoBId + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", PRODUCT_RICE_ID), managerHeaders()), Map.class);
        assertThat(addProductFail.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void duplicateBatchTarget_returns409() {
        String code = "PROMO-BATCH-DUP-01";
        var createRes = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), managerHeaders()), Map.class);
        String promoId = (String) createRes.getBody().get("id");

        var add1 = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), managerHeaders()), Void.class);
        assertThat(add1.getStatusCode().value()).isEqualTo(201);

        var addDup = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), managerHeaders()), Map.class);
        assertThat(addDup.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void removeNonExistentBatch_returns404() {
        String code = "PROMO-BATCH-404-01";
        var createRes = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), managerHeaders()), Map.class);
        String promoId = (String) createRes.getBody().get("id");

        var delRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches/" + UUID.randomUUID(),
                HttpMethod.DELETE, new HttpEntity<>(managerHeaders()), Map.class);
        assertThat(delRes.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void salesRoleCannotManageBatches_returns403() {
        String code = "PROMO-BATCH-RBAC-01";
        var createRes = rest.exchange("/api/v1/promotions", HttpMethod.POST,
                new HttpEntity<>(validPromoJson(code), managerHeaders()), Map.class);
        String promoId = (String) createRes.getBody().get("id");

        var postRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("productBatchId", BATCH_RICE_ID), salesHeaders()), Map.class);
        assertThat(postRes.getStatusCode().value()).isEqualTo(403);

        var delRes = rest.exchange("/api/v1/promotions/" + promoId + "/batches/" + BATCH_RICE_ID,
                HttpMethod.DELETE, new HttpEntity<>(salesHeaders()), Map.class);
        assertThat(delRes.getStatusCode().value()).isEqualTo(403);
    }

    // ===== Helper =====

    private String validPromoJson(String code) {
        return """
                {"code":"%s","name":"Khuyến mãi test","discountType":"PERCENT","discountValue":10,
                 "startsAt":"2026-10-01T00:00:00Z","endsAt":"2026-10-31T00:00:00Z","status":"DRAFT"}"""
                .formatted(code);
    }
}
