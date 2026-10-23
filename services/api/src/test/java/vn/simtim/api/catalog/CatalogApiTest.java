package vn.simtim.api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
    private final List<String> crudFixtures = new ArrayList<>();

    @AfterEach
    void removeCrudFixtures() {
        for (int i = crudFixtures.size() - 1; i >= 0; i--) {
            var response = call(crudFixtures.get(i), HttpMethod.DELETE, null);
            assertThat(response.getStatusCode().value()).isIn(204, 404);
        }
        crudFixtures.clear();
    }

    private ResponseEntity<Map> call(String path, HttpMethod method, Object body) {
        return rest.exchange(path, method, new HttpEntity<>(body, orgHeaders()), Map.class);
    }

    private String createFixture(String path, Object body) {
        var response = call(path, HttpMethod.POST, body);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        String url = path + "/" + response.getBody().get("id");
        crudFixtures.add(url);
        return url;
    }

    private String createProductFixture() {
        return createFixture("/api/v1/products", Map.of(
                "categoryId", CATEGORY_ID, "baseUnitId", UNIT_EA_ID,
                "sku", "CRUD-" + UUID.randomUUID(), "name", "CRUD fixture",
                "salePrice", 25_000, "tracksExpiry", false));
    }

    @Test
    void unitsSupportFullCrudAndPersistChanges() {
        String path = createFixture("/api/v1/units",
                Map.of("code", "CRUD-UNIT", "name", "Original", "precisionScale", 0));
        var list = rest.exchange("/api/v1/units", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), List.class);
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        assertThat(list.getBody()).contains(call(path, HttpMethod.GET, null).getBody());
        assertThat(call(path, HttpMethod.PUT,
                Map.of("code", "CRUD-EDIT", "name", "Edited", "precisionScale", 3))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(call(path, HttpMethod.GET, null).getBody())
                .containsEntry("code", "CRUD-EDIT").containsEntry("name", "Edited")
                .containsEntry("precisionScale", 3);
        assertThat(call(path, HttpMethod.DELETE, null).getStatusCode().value()).isEqualTo(204);
        assertThat(call(path, HttpMethod.GET, null).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void unitValidationAndDuplicatesNeverChangeStoredData() {
        String path = createFixture("/api/v1/units",
                Map.of("code", "CRUD-VALID", "name", "Original", "precisionScale", 0));
        var before = call(path, HttpMethod.GET, null).getBody();
        for (int scale : List.of(-1, 4)) {
            var body = Map.of("code", "CRUD-INVALID", "name", "Invalid", "precisionScale", scale);
            for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT)) {
                var response = call(method == HttpMethod.POST ? "/api/v1/units" : path, method, body);
                assertThat(response.getStatusCode().value()).isEqualTo(422);
                assertThat(((Map<?, ?>) response.getBody().get("errors")).get("precisionScale")).isNotNull();
            }
        }
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT)) {
            assertThat(call(method == HttpMethod.POST ? "/api/v1/units" : path, method,
                    Map.of("code", "ea", "name", "Duplicate", "precisionScale", 0))
                    .getStatusCode().value()).isEqualTo(409);
        }
        assertThat(call(path, HttpMethod.GET, null).getBody()).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from catalog.units where code='CRUD-INVALID'", Long.class))
                .isZero();
    }

    @Test
    void barcodesSupportFullCrudAndSearchUsesUpdatedValue() {
        String product = createProductFixture();
        String path = createFixture(product + "/barcodes", Map.of("barcode", "000-CRUD-OLD", "isPrimary", true));
        assertThat(call(path, HttpMethod.GET, null).getBody()).containsEntry("barcode", "000-CRUD-OLD");
        var list = rest.exchange(product + "/barcodes", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), List.class);
        assertThat(list.getBody()).hasSize(1).contains(call(path, HttpMethod.GET, null).getBody());
        var body = Map.of("barcode", "000-CRUD-NEW", "isPrimary", false);
        // Repeating the same update must not report a duplicate of itself.
        for (int i = 0; i < 2; i++) {
            assertThat(call(path, HttpMethod.PUT, body).getStatusCode().value()).isEqualTo(200);
        }
        assertThat(call(path, HttpMethod.GET, null).getBody())
                .containsEntry("barcode", "000-CRUD-NEW").containsEntry("isPrimary", false);
        var oldSearch = rest.exchange("/api/v1/products/search?barcode=000-CRUD-OLD", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), List.class);
        var newSearch = rest.exchange("/api/v1/products/search?barcode=000-CRUD-NEW", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), List.class);
        assertThat(oldSearch.getBody()).isEmpty();
        assertThat(newSearch.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) newSearch.getBody().get(0)).get("id"))
                .isEqualTo(product.substring(product.lastIndexOf('/') + 1));
        assertThat(call(path, HttpMethod.DELETE, null).getStatusCode().value()).isEqualTo(204);
        assertThat(call(path, HttpMethod.GET, null).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void barcodeDuplicateAndPrimaryConflictsRollback() {
        String product = createProductFixture();
        String primary = createFixture(product + "/barcodes", Map.of("barcode", "CRUD-PRIMARY", "isPrimary", true));
        String secondary = createFixture(product + "/barcodes", Map.of("barcode", "CRUD-SECONDARY", "isPrimary", false));
        var before = call(secondary, HttpMethod.GET, null).getBody();
        assertThat(call(secondary, HttpMethod.PUT, Map.of("barcode", "8930000000001", "isPrimary", false))
                .getStatusCode().value()).isEqualTo(409);
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT)) {
            var response = call(method == HttpMethod.POST ? product + "/barcodes" : secondary,
                    method, Map.of("barcode", "CRUD-ANOTHER-PRIMARY", "isPrimary", true));
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).containsEntry("code", "CATALOG_CONSTRAINT_CONFLICT");
        }
        assertThat(call(secondary, HttpMethod.GET, null).getBody()).isEqualTo(before);
        assertThat(call(primary, HttpMethod.GET, null).getBody()).containsEntry("isPrimary", true);
        var list = rest.exchange(product + "/barcodes", HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), List.class);
        assertThat(list.getBody()).hasSize(2);
        // Explicitly demote the old primary before promoting another barcode.
        assertThat(call(primary, HttpMethod.PUT, Map.of("barcode", "CRUD-PRIMARY", "isPrimary", false))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(call(secondary, HttpMethod.PUT, Map.of("barcode", "CRUD-SECONDARY", "isPrimary", true))
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void barcodeWritesEnforcePermissionsScopeOwnershipAndValidation() {
        String product = createProductFixture();
        String path = createFixture(product + "/barcodes", Map.of("barcode", "CRUD-SECURE", "isPrimary", false));
        var body = Map.of("barcode", "CRUD-REJECTED", "isPrimary", false);
        var before = call(path, HttpMethod.GET, null).getBody();
        assertThat(rest.exchange(path, HttpMethod.GET, new HttpEntity<>(sessionHeaders(salesToken)), Map.class)
                .getStatusCode().value()).isEqualTo(200);
        for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)) {
            assertThat(rest.exchange(path, method, new HttpEntity<>(body, scopeHeaders()), Map.class)
                    .getStatusCode().value()).isEqualTo(401);
            var spoofed = orgHeaders();
            spoofed.set("X-Organization-Id", UUID.randomUUID().toString());
            assertThat(rest.exchange(path, method, new HttpEntity<>(body, spoofed), Map.class)
                    .getStatusCode().value()).isEqualTo(403);
            String wrongProduct = "/api/v1/products/" + PRODUCT_RICE_ID + "/barcodes/"
                    + before.get("id");
            assertThat(call(wrongProduct, method, body).getStatusCode().value()).isEqualTo(409);
        }
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
            String target = method == HttpMethod.POST ? product + "/barcodes" : path;
            assertThat(rest.exchange(target, method, new HttpEntity<>(body, sessionHeaders(salesToken)), Map.class)
                    .getStatusCode().value()).isEqualTo(403);
        }
        for (String invalid : List.of(" ", "x".repeat(101))) {
            assertThat(call(path, HttpMethod.PUT, Map.of("barcode", invalid, "isPrimary", false))
                    .getStatusCode().value()).isEqualTo(422);
        }
        assertThat(call(path, HttpMethod.GET, null).getBody()).isEqualTo(before);
    }

    @Test
    void missingCrudTargetsReturn404() {
        for (String resource : List.of("categories", "units", "products", "suppliers")) {
            String path = "/api/v1/" + resource + "/" + UUID.randomUUID();
            Object body = resource.equals("products")
                    ? Map.of("sku", "MISSING", "name", "Missing", "categoryId", CATEGORY_ID,
                            "baseUnitId", UNIT_EA_ID, "salePrice", 25_000)
                    : Map.of("code", "MISSING", "name", "Missing");
            for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)) {
                assertThat(call(path, method, body).getStatusCode().value()).isEqualTo(404);
            }
        }
        String path = "/api/v1/products/" + PRODUCT_RICE_ID + "/barcodes/" + UUID.randomUUID();
        for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)) {
            assertThat(call(path, method, Map.of("barcode", "MISSING", "isPrimary", false))
                    .getStatusCode().value()).isEqualTo(404);
        }
    }

    @Test
    void referencedCatalogDeletesReturnConflictAndKeepData() {
        // Seed product has a price/barcode; category and unit are used by products.
        for (String path : List.of("/api/v1/categories/" + CATEGORY_ID,
                "/api/v1/units/" + UNIT_EA_ID, "/api/v1/products/" + PRODUCT_RICE_ID)) {
            var before = call(path, HttpMethod.GET, null).getBody();
            var response = call(path, HttpMethod.DELETE, null);
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).containsEntry("code", "CATALOG_CONSTRAINT_CONFLICT");
            assertThat(response.getBody().get("detail").toString()).doesNotContain("SQL", "constraint [", "org.hibernate");
            assertThat(call(path, HttpMethod.GET, null).getBody()).isEqualTo(before);
        }
        assertThat(jdbc.queryForObject("select count(*) from catalog.product_prices where product_id=?",
                Long.class, UUID.fromString(PRODUCT_RICE_ID))).isPositive();
    }

    @Test
    void referencedSupplierDeleteKeepsReceipt() {
        String supplier = createFixture("/api/v1/suppliers", Map.of("code", "CRUD-RECEIPT", "name", "Referenced supplier"));
        UUID supplierId = UUID.fromString(supplier.substring(supplier.lastIndexOf('/') + 1));
        UUID receiptId = UUID.randomUUID();
        jdbc.update("""
                insert into inventory.goods_receipts(id,organization_id,store_id,supplier_id,status,
                    received_at,confirmed_by,client_operation_id,idempotency_key,payload_hash)
                values(?,?,?,?,'CONFIRMED',now(),?,?,?,?)
                """, receiptId, UUID.fromString(ORG_ID), UUID.fromString(STORE_ID), supplierId,
                fixtureUsers.get(0), UUID.randomUUID(), UUID.randomUUID(), new byte[]{1});
        try {
            var response = call(supplier, HttpMethod.DELETE, null);
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).containsEntry("code", "CATALOG_CONSTRAINT_CONFLICT");
            assertThat(call(supplier, HttpMethod.GET, null).getStatusCode().value()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select supplier_id from inventory.goods_receipts where id=?",
                    UUID.class, receiptId)).isEqualTo(supplierId);
        } finally {
            jdbc.update("delete from inventory.goods_receipts where id=?", receiptId);
        }
    }

    @Test
    void categoryCyclesAndDeletingParentsAreRejected() {
        String parent = createFixture("/api/v1/categories", Map.of("code", "CRUD-PARENT", "name", "Parent"));
        String parentId = parent.substring(parent.lastIndexOf('/') + 1);
        String child = createFixture("/api/v1/categories", Map.of("code", "CRUD-CHILD", "name", "Child", "parentId", parentId));
        String childId = child.substring(child.lastIndexOf('/') + 1);
        String grandchild = createFixture("/api/v1/categories",
                Map.of("code", "CRUD-GRANDCHILD", "name", "Grandchild", "parentId", childId));
        String grandchildId = grandchild.substring(grandchild.lastIndexOf('/') + 1);
        for (String invalidParent : List.of(parentId, childId, grandchildId)) {
            assertThat(call(parent, HttpMethod.PUT,
                    Map.of("code", "CRUD-PARENT", "name", "Parent", "parentId", invalidParent))
                    .getStatusCode().value()).isEqualTo(409);
        }
        assertThat(call(parent, HttpMethod.GET, null).getBody()).containsEntry("parentId", null);
        assertThat(call(parent, HttpMethod.DELETE, null).getStatusCode().value()).isEqualTo(409);
        assertThat(call(child, HttpMethod.GET, null).getBody()).containsEntry("parentId", parentId);
        // Moving a descendant to the root remains a valid update.
        assertThat(call(grandchild, HttpMethod.PUT, Map.of("code", "CRUD-GRANDCHILD", "name", "Root"))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(call(grandchild, HttpMethod.GET, null).getBody()).containsEntry("parentId", null);
    }

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
                 "name":"Trùng SKU","salePrice":25000,"tracksExpiry":false,"status":"ACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var res = rest.exchange("/api/v1/products", HttpMethod.POST,
                new HttpEntity<>(body, orgHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void addProductBarcode_conflict_onDuplicateBarcode_withoutCreatingRecord() {
        String url = "/api/v1/products/" + PRODUCT_RICE_ID + "/barcodes";
        var before = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(before.getStatusCode().value()).isEqualTo(200);

        var duplicate = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<>("""
                        {"barcode":"8930000000001","isPrimary":false}""", orgHeaders()),
                Map.class);
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);

        var after = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(orgHeaders()), Object[].class);
        assertThat(after.getStatusCode().value()).isEqualTo(200);
        assertThat(after.getBody()).hasSameSizeAs(before.getBody());
    }

    @Test
    void createProduct_then_updateAndDelete() {
        var createBody = """
                {"categoryId":"%s","baseUnitId":"%s","sku":"ST-TEST-99",
                 "name":"Sản phẩm test","salePrice":42000,
                 "tracksExpiry":false,"status":"ACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var created = rest.exchange("/api/v1/products", HttpMethod.POST,
                new HttpEntity<>(createBody, orgHeaders()), Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var id = created.getBody().get("id").toString();
        assertThat(created.getBody()).containsEntry("salePrice", 42000);
        assertThat(jdbc.queryForObject("""
                select sale_price from catalog.product_prices
                where organization_id=?::uuid and store_id=?::uuid and product_id=?::uuid
                  and effective_to is null
                """, Long.class, ORG_ID, STORE_ID, id)).isEqualTo(42_000L);

        // Cập nhật
        var updateBody = """
                {"categoryId":"%s","baseUnitId":"%s","sku":"ST-TEST-99",
                 "name":"Đã sửa","salePrice":45000,
                 "tracksExpiry":true,"status":"INACTIVE"}"""
                .formatted(CATEGORY_ID, UNIT_EA_ID);
        var updated = rest.exchange("/api/v1/products/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateBody, orgHeaders()), Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("tracksExpiry", true).containsEntry("salePrice", 45000);
        assertThat(jdbc.queryForObject("""
                select sale_price from catalog.product_prices
                where organization_id=?::uuid and store_id=?::uuid and product_id=?::uuid
                  and effective_to is null
                """, Long.class, ORG_ID, STORE_ID, id)).isEqualTo(45_000L);

        // Xóa
        var del = rest.exchange("/api/v1/products/" + id, HttpMethod.DELETE,
                new HttpEntity<>(orgHeaders()), Void.class);
        assertThat(del.getStatusCode().value()).isEqualTo(204);
        assertThat(jdbc.queryForObject("select count(*) from catalog.product_prices where product_id=?::uuid",
                Long.class, id)).isZero();
    }

    @Test
    void createProduct_requiresNonNegativeSalePrice() {
        for (String priceField : List.of("", ",\"salePrice\":-1")) {
            var body = """
                    {"categoryId":"%s","baseUnitId":"%s","sku":"PRICE-%s",
                     "name":"Thiếu giá","tracksExpiry":false%s}"""
                    .formatted(CATEGORY_ID, UNIT_EA_ID, UUID.randomUUID(), priceField);
            var response = rest.exchange("/api/v1/products", HttpMethod.POST,
                    new HttpEntity<>(body, orgHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(422);
            assertThat(((Map<?, ?>) response.getBody().get("errors")).containsKey("salePrice")).isTrue();
        }
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

    @Test
    void uploadProductImage_and_getImage_succeeds() {
        String productUrl = createProductFixture();
        String productId = productUrl.substring(productUrl.lastIndexOf('/') + 1);

        // Multipart upload
        byte[] imageBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }; // PNG header
        org.springframework.util.MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource res = new org.springframework.core.io.ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() { return "sample.png"; }
        };
        var partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        parts.add("image", new HttpEntity<>(res, partHeaders));

        var uploadHeaders = new HttpHeaders();
        uploadHeaders.set("X-Organization-Id", ORG_ID);
        uploadHeaders.setBearerAuth(stockToken);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        var uploadResp = rest.exchange("/api/v1/products/" + productId + "/image", HttpMethod.PUT,
                new HttpEntity<>(parts, uploadHeaders), Map.class);
        assertThat(uploadResp.getStatusCode().value()).isEqualTo(200);
        assertThat(uploadResp.getBody()).containsEntry("imageUrl", "/api/v1/products/" + productId + "/image");

        // Public GET without auth headers
        var getResp = rest.getForEntity("/api/v1/products/" + productId + "/image", byte[].class);
        assertThat(getResp.getStatusCode().value()).isEqualTo(200);
        assertThat(getResp.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(getResp.getHeaders().getETag()).isNotNull();
        assertThat(getResp.getHeaders().getCacheControl()).contains("no-cache");
        assertThat(getResp.getBody()).isEqualTo(imageBytes);

        // Upload 2 MB image (verifies spring.servlet.multipart.max-file-size=5MB allows files > 1MB)
        byte[] largeImageBytes = new byte[2 * 1024 * 1024];
        largeImageBytes[0] = (byte) 0x89;
        largeImageBytes[1] = 0x50;
        largeImageBytes[2] = 0x4E;
        largeImageBytes[3] = 0x47;
        org.springframework.util.MultiValueMap<String, Object> largeParts = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource largeRes = new org.springframework.core.io.ByteArrayResource(largeImageBytes) {
            @Override
            public String getFilename() { return "large.png"; }
        };
        var largePartHeaders = new HttpHeaders();
        largePartHeaders.setContentType(MediaType.IMAGE_PNG);
        largeParts.add("image", new HttpEntity<>(largeRes, largePartHeaders));

        var largeUploadResp = rest.exchange("/api/v1/products/" + productId + "/image", HttpMethod.PUT,
                new HttpEntity<>(largeParts, uploadHeaders), Map.class);
        assertThat(largeUploadResp.getStatusCode().value()).isEqualTo(200);

        var largeGetResp = rest.getForEntity("/api/v1/products/" + productId + "/image", byte[].class);
        assertThat(largeGetResp.getStatusCode().value()).isEqualTo(200);
        assertThat(largeGetResp.getBody()).hasSize(2 * 1024 * 1024);

        // Delete image
        var delHeaders = new HttpHeaders();
        delHeaders.set("X-Organization-Id", ORG_ID);
        delHeaders.setBearerAuth(stockToken);
        var delResp = rest.exchange("/api/v1/products/" + productId + "/image", HttpMethod.DELETE,
                new HttpEntity<>(delHeaders), Map.class);
        assertThat(delResp.getStatusCode().value()).isEqualTo(200);
        assertThat(delResp.getBody().get("imageUrl")).isNull();

        // GET after delete should return 404
        var getAfterDel = rest.getForEntity("/api/v1/products/" + productId + "/image", byte[].class);
        assertThat(getAfterDel.getStatusCode().value()).isEqualTo(404);
    }
}
