package vn.simtim.api.catalog;

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
 * Integration test cho BE-03: API danh mục, sản phẩm và nhà cung cấp.
 * Dùng Testcontainers PostgreSQL 17 (giống CI) và profile demo để seed dữ liệu.
 */
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogApiTest {

    static final String ORG_ID = "10000000-0000-0000-0000-000000000001";
    static final String STORE_ID = "10000000-0000-0000-0000-000000000002";
    static final String CATEGORY_ID = "10000000-0000-0000-0000-000000000021";
    static final String UNIT_EA_ID = "10000000-0000-0000-0000-000000000031";
    static final String PRODUCT_RICE_ID = "10000000-0000-0000-0000-000000000041";
    static final String SUPPLIER_ID = "10000000-0000-0000-0000-000000000061";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_catalog_test");

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

    @AfterAll
    void stop() {
        try {
            // Remove only accounts owned by this suite, including with a shared disposable DB.
            for (UUID id : fixtureUsers) {
                jdbc.update("delete from iam.auth_sessions where user_id=?", id);
                jdbc.update("delete from iam.user_roles where user_id=?", id);
                jdbc.update("delete from iam.users where id=?", id);
            }
        } finally {
            if (postgres.isRunning()) postgres.stop();
        }
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordHasher passwords;
    private final List<UUID> fixtureUsers = new ArrayList<>();
    private static final String PASSWORD = "catalog-fixture-password-123";
    private String stockToken;
    private String salesToken;

    @BeforeAll
    void authenticateFixtures() {
        String hash = passwords.hash(PASSWORD);
        stockToken = createSession("STOCK", hash);
        salesToken = createSession("SALES", hash);
    }

    private String createSession(String role, String hash) {
        UUID id = UUID.randomUUID();
        String username = "catalog_" + role.toLowerCase(Locale.ROOT) + "_" + id;
        jdbc.update("""
                insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                values(?,?,?,?,?,'ACTIVE')
                """, id, UUID.fromString(ORG_ID), username, hash, "Catalog fixture " + role);
        fixtureUsers.add(id);
        assertThat(jdbc.update("""
                insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                select organization_id,?,id,?,now(),? from iam.roles
                where organization_id=? and code=?
                """, id, UUID.fromString(STORE_ID), id, UUID.fromString(ORG_ID), role)).isEqualTo(1);
        var response = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(Map.of(
                "organizationCode", "SIMTIM", "storeCode", "MAIN",
                "username", username, "password", PASSWORD), scopeHeaders()), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().containsEntry("tokenType", "Bearer");
        String token = (String) response.getBody().get("accessToken");
        assertThat(token).hasSize(43);
        return token;
    }

    private HttpHeaders scopeHeaders() {
        var h = new HttpHeaders();
        h.set("X-Organization-Id", ORG_ID);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders sessionHeaders(String token) {
        var h = scopeHeaders();
        h.setBearerAuth(token);
        return h;
    }

    HttpHeaders orgHeaders() {
        return sessionHeaders(stockToken);
    }

    @Test
    void catalogRequestsWithoutBearerAreRejected() {
        for (String path : List.of("categories", "units", "products", "suppliers")) {
            var response = rest.exchange("/api/v1/" + path, HttpMethod.GET,
                    new HttpEntity<>(scopeHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).containsEntry("code", "UNAUTHENTICATED");
        }
    }

    @Test
    void salesCanReadCatalogButCannotCreateUpdateOrDelete() {
        for (String path : List.of("categories", "units", "products", "suppliers")) {
            String url = "/api/v1/" + path;
            var read = rest.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(sessionHeaders(salesToken)), String.class);
            assertThat(read.getStatusCode().value()).isEqualTo(200);
            for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
                String target = method == HttpMethod.POST ? url : url + "/" + UUID.randomUUID();
                var denied = rest.exchange(target, method,
                        new HttpEntity<>("{}", sessionHeaders(salesToken)), Map.class);
                assertThat(denied.getStatusCode().value()).isEqualTo(403);
                assertThat(denied.getBody()).containsEntry("code", "FORBIDDEN");
            }
        }
    }

    @Test
    void catalogRejectsOrganizationAndStoreOutsideSessionScope() {
        for (String scope : List.of("X-Organization-Id", "X-Store-Id")) {
            var headers = orgHeaders();
            headers.set(scope, UUID.randomUUID().toString());
            for (String path : List.of("categories", "units", "products", "suppliers")) {
                var response = rest.exchange("/api/v1/" + path, HttpMethod.GET,
                        new HttpEntity<>(headers), Map.class);
                assertThat(response.getStatusCode().value()).isEqualTo(403);
                assertThat(response.getBody()).containsEntry("code", "FORBIDDEN");
            }
        }
    }

    // ===== Categories =====

    @Test
    void listCategories_returnsDemoData() {
        var res = rest.exchange("/api/v1/categories", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void getCategory_found() {
        var res = rest.exchange("/api/v1/categories/" + CATEGORY_ID, HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).containsEntry("code", "GROCERY");
    }

    @Test
    void getCategory_notFound_returns404() {
        var res = rest.exchange("/api/v1/categories/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createCategory_then_conflict_on_duplicateCode() {
        var body = """
                {"code":"CAT-NEW-01","name":"Danh mục mới","status":"ACTIVE"}""";
        var create = rest.exchange("/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(create.getStatusCode().value()).isEqualTo(201);
        assertThat(create.getBody()).containsKey("id");

        // Tạo lại với cùng mã → 409
        var dup = rest.exchange("/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(dup.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void createCategory_invalidPayload_returns422() {
        var body = """
                {"code":"","name":""}""";
        var res = rest.exchange("/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
        assertThat(res.getBody()).containsKey("errors");
    }

    @Test
    void updateAndDeleteCategory() {
        // Tạo
        var body = """
                {"code":"CAT-DEL-01","name":"Sẽ xóa","status":"ACTIVE"}""";
        var created = rest.exchange("/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();

        // Cập nhật
        var updated = rest.exchange("/api/v1/categories/" + id, HttpMethod.PUT,
                new HttpEntity<>("""
                        {"code":"CAT-DEL-01","name":"Đã sửa","status":"INACTIVE"}""", orgHeaders()),
                Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("status", "INACTIVE");

        // Xóa
        var del = rest.exchange("/api/v1/categories/" + id, HttpMethod.DELETE,
                new HttpEntity<>(orgHeaders()), Void.class);
        assertThat(del.getStatusCode().value()).isEqualTo(204);

        // Xác nhận không còn
        var gone = rest.exchange("/api/v1/categories/" + id, HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Map.class);
        assertThat(gone.getStatusCode().value()).isEqualTo(404);
    }

    // ===== Products =====

    @Test
    void listProducts_returnsDemoData() {
        var res = rest.exchange("/api/v1/products", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(2);
    }

    @Test
    void searchProduct_bySku() {
        var res = rest.exchange("/api/v1/products/search?sku=ST-RICE-01", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void searchProduct_byBarcode() {
        var res = rest.exchange("/api/v1/products/search?barcode=8930000000001", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void searchProduct_byName() {
        var res = rest.exchange("/api/v1/products/search?name=Gạo", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void createProduct_conflict_on_duplicateSku() {
        var body = """
                {"categoryId":"%s","baseUnitId":"%s","sku":"ST-RICE-01",
                 "name":"Trùng SKU","tracksExpiry":false,"status":"ACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var res = rest.exchange("/api/v1/products", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void createProduct_then_updateAndDelete() {
        var createBody = """
                {"categoryId":"%s","baseUnitId":"%s","sku":"ST-TEST-99",
                 "name":"Sản phẩm test","tracksExpiry":false,"status":"ACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var created = rest.exchange("/api/v1/products", HttpMethod.POST,
                new HttpEntity<>(createBody, orgHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();

        // Cập nhật
        var updateBody = """
                {"categoryId":"%s","baseUnitId":"%s","sku":"ST-TEST-99",
                 "name":"Đã sửa","tracksExpiry":true,"status":"INACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var updated = rest.exchange("/api/v1/products/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, orgHeaders()), Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("tracksExpiry", true);

        // Xóa
        var del = rest.exchange("/api/v1/products/" + id, HttpMethod.DELETE,
                new HttpEntity<>(orgHeaders()), Void.class);
        assertThat(del.getStatusCode().value()).isEqualTo(204);
    }

    // ===== Suppliers =====

    @Test
    void listSuppliers_returnsDemoData() {
        var res = rest.exchange("/api/v1/suppliers", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void getSupplier_found() {
        var res = rest.exchange("/api/v1/suppliers/" + SUPPLIER_ID, HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).containsEntry("code", "SUP-001");
    }

    @Test
    void createSupplier_conflict_on_duplicateCode() {
        var body = """
                {"code":"SUP-001","name":"Trùng mã","status":"ACTIVE"}""";
        var res = rest.exchange("/api/v1/suppliers", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void createSupplier_invalidEmail_returns422() {
        var body = """
                {"code":"SUP-999","name":"Test","email":"not-an-email","status":"ACTIVE"}""";
        var res = rest.exchange("/api/v1/suppliers", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void createSupplier_then_updateAndDelete() {
        var body = """
                {"code":"SUP-NEW-99","name":"NCC Mới","phone":"0901234567",
                 "email":"ncc@example.com","status":"ACTIVE"}""";
        var created = rest.exchange("/api/v1/suppliers", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();

        // Cập nhật
        var updated = rest.exchange("/api/v1/suppliers/" + id, HttpMethod.PUT,
                new HttpEntity<>("""
                        {"code":"SUP-NEW-99","name":"NCC Đã sửa","status":"INACTIVE"}""",
                        orgHeaders()), Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("status", "INACTIVE");

        // Xóa
        var del = rest.exchange("/api/v1/suppliers/" + id, HttpMethod.DELETE,
                new HttpEntity<>(orgHeaders()), Void.class);
        assertThat(del.getStatusCode().value()).isEqualTo(204);
    }
}
