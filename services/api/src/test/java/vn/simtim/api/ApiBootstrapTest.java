package vn.simtim.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Import(ApiBootstrapTest.ProtectedRoutes.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiBootstrapTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String externalUrl = System.getenv("SIMTIM_TEST_DB_URL");
        if (externalUrl == null || externalUrl.isBlank()) {
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> System.getenv("SIMTIM_TEST_DB_USER"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("SIMTIM_TEST_DB_PASSWORD", ""));
        }
    }

    @AfterAll
    static void stopContainer() {
        if (postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired vn.simtim.api.auth.domain.PasswordHasher passwords;
    // Uses the production catalog policy on a dedicated test route, alongside BE-03 controllers.
    private static final String CATALOG_PERMISSION_PROBE = "/api/v1/products/__be02_security_fixture";
    private static final String PASSWORD = "fixture-password-123";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder(12).encode(PASSWORD);

    @TestConfiguration(proxyBeanMethods = false)
    static class ProtectedRoutes {
        @Bean TestCatalogController testCatalogController() { return new TestCatalogController(); }
    }

    @RestController
    static class TestCatalogController {
        @GetMapping(CATALOG_PERMISSION_PROBE) Map<String, UUID> read(Authentication authentication) {
            return Map.of("organizationId", ((SessionPrincipal) authentication.getPrincipal()).organizationId());
        }
        @PostMapping(CATALOG_PERMISSION_PROBE) ResponseEntity<Void> write() {
            return ResponseEntity.noContent().build();
        }
    }

    private void accounts() {
        int index = 101;
        for (String role : new String[]{"SALES", "STOCK", "MANAGER", "ADMIN"}) {
            UUID id = UUID.fromString("30000000-0000-0000-0000-000000000" + index++);
            jdbcTemplate.update("""
                    insert into iam.users(id,organization_id,username,password_hash,full_name,status,training_enabled)
                    values(?,'10000000-0000-0000-0000-000000000001',?,?,?,'ACTIVE',?)
                    """, id, "be02_" + role, PASSWORD_HASH, role, role.equals("SALES"));
            jdbcTemplate.update("""
                    insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                    select organization_id,?,id,'10000000-0000-0000-0000-000000000002',now(),?
                    from iam.roles where organization_id='10000000-0000-0000-0000-000000000001' and code=?
                    """, id, id, role);
        }
    }

    private String login(String role) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("organizationCode", "SIMTIM", "storeCode", "MAIN",
                        "username", " BE02_" + role + " ", "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        var body = mapper.readTree(response);
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(java.time.Instant.parse(body.get("expiresAt").asText())).isAfter(java.time.Instant.now());
        assertThat(body.get("session").get("organizationId").asText()).isEqualTo("10000000-0000-0000-0000-000000000001");
        assertThat(body.get("session").get("storeId").asText()).isEqualTo("10000000-0000-0000-0000-000000000002");
        assertThat(body.get("session").get("roles").toString()).contains(role).doesNotContain("LEARNER");
        assertThat(body.get("session").toString()).doesNotContain("passwordHash");
        return body.get("accessToken").asText();
    }

    @Test @Transactional
    void wrongCredentialsHaveGenericErrorAndSessionStoresOnlyHash() throws Exception {
        accounts();
        String known = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("organizationCode", "SIMTIM", "storeCode", "MAIN",
                        "username", "be02_SALES", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("organizationCode", "SIMTIM", "storeCode", "MAIN",
                        "username", "unknown", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown).contains("INVALID_CREDENTIALS");
        String token = login("SALES");
        assertThat(token).hasSize(43);
        byte[] stored = jdbcTemplate.queryForObject("select token_hash from iam.auth_sessions where user_id='30000000-0000-0000-0000-000000000101'", byte[].class);
        assertThat(stored).hasSize(32).isNotEqualTo(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThat(PASSWORD_HASH).isNotEqualTo(PASSWORD);
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, PASSWORD_HASH)).isTrue();
    }

    @Test @Transactional
    void fourRolesEnforceCatalogPermissionsAndTrainingDoesNotEscalate() throws Exception {
        accounts();
        mvc.perform(get(CATALOG_PERMISSION_PROBE)).andExpect(status().isUnauthorized());
        for (String role : new String[]{"SALES", "STOCK", "MANAGER", "ADMIN"}) {
            String token = login(role);
            mvc.perform(get(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            mvc.perform(post(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token))
                    .andExpect(status().is(role.equals("SALES") ? 403 : 204));
        }
    }

    @Test @Transactional
    void logoutAndExpiryInvalidateBearerSessions() throws Exception {
        accounts();
        String token = login("MANAGER");
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        token = login("STOCK");
        jdbcTemplate.update("update iam.auth_sessions set issued_at=now()-interval '2 hours', expires_at=now()-interval '1 hour' where user_id='30000000-0000-0000-0000-000000000102'");
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test @Transactional
    void rejectsSpoofedTenantAndRechecksAccountAndRoleState() throws Exception {
        accounts();
        String token = login("ADMIN");
        mvc.perform(get(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token)
                .header("X-Organization-Id", UUID.randomUUID().toString())).andExpect(status().isForbidden());
        mvc.perform(get(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token)
                .header("X-Store-Id", UUID.randomUUID().toString())).andExpect(status().isForbidden());
        jdbcTemplate.update("update iam.users set status='LOCKED' where username='be02_ADMIN'");
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        jdbcTemplate.update("update iam.users set status='ACTIVE' where username='be02_ADMIN'");
        jdbcTemplate.update("delete from iam.user_roles where user_id='30000000-0000-0000-0000-000000000104'");
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test @Transactional
    void requiresStoreAssignmentAndReloadsPermissionGrants() throws Exception {
        accounts();
        jdbcTemplate.update("""
                insert into core.stores(id,organization_id,code,name,status)
                values('30000000-0000-0000-0000-000000000201',
                '10000000-0000-0000-0000-000000000001','SECOND','Second store','ACTIVE')
                """);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("organizationCode", "SIMTIM", "storeCode", "SECOND",
                        "username", "be02_STOCK", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        String token = login("STOCK");
        jdbcTemplate.update("""
                delete from iam.role_permissions rp using iam.roles r,iam.permissions p
                where rp.role_id=r.id and rp.permission_id=p.id and r.code='STOCK' and p.code='catalog.write'
                """);
        mvc.perform(post(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get(CATALOG_PERMISSION_PROBE).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        jdbcTemplate.update("update core.stores set status='INACTIVE' where code='MAIN'");
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test @Transactional
    void demoBootstrapRequiresSuppliedPasswordAndDoesNotResetExistingAccounts() throws Exception {
        var empty = new vn.simtim.api.auth.infrastructure.DemoAccounts(jdbcTemplate, passwords, "");
        empty.run(null);
        assertThat(jdbcTemplate.queryForObject("select count(*) from iam.users", Integer.class)).isZero();
        var seeder = new vn.simtim.api.auth.infrastructure.DemoAccounts(jdbcTemplate, passwords, PASSWORD);
        seeder.run(null);
        String hash = jdbcTemplate.queryForObject("select password_hash from iam.users where username='sales'", String.class);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        seeder.run(null);
        assertThat(jdbcTemplate.queryForObject("select count(*) from iam.users", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select password_hash from iam.users where username='sales'", String.class)).isEqualTo(hash);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("organizationCode", "SIMTIM", "storeCode", "MAIN",
                        "username", "sales", "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMalformedLoginAndUnsupportedAuthentication() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Basic eDp4"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequestsAreRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/api/v1/auth/login").servletPath("/api/v1/auth/login")
                    .with(request -> { request.setRemoteAddr("198.51.100.20"); return request; })
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/auth/login").servletPath("/api/v1/auth/login")
                .with(request -> { request.setRemoteAddr("198.51.100.20"); return request; })
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void migrationAndDemoSeedServeHealth() {
        var response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_type = 'BASE TABLE' "
                        + "and table_schema in ('core', 'iam', 'catalog')", Integer.class))
                .isEqualTo(14);
        assertThat(jdbcTemplate.queryForObject(
                "select success from flyway_schema_history where version = '3'", Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select success from flyway_schema_history where script = 'R__demo_seed.sql' order by installed_rank desc limit 1",
                Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("select count(*) from iam.roles", Integer.class))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from iam.roles where code = 'LEARNER'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from catalog.products", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from catalog.product_prices", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void rejectsFifthRoleAndOverlappingPrice() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into iam.roles(id, organization_id, code, name) values "
                        + "('10000000-0000-0000-0000-000000000099', "
                        + "'10000000-0000-0000-0000-000000000001', 'LEARNER', 'Learner')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into catalog.product_prices "
                        + "(id, organization_id, store_id, product_id, sale_price, effective_from) values "
                        + "('10000000-0000-0000-0000-000000000098', "
                        + "'10000000-0000-0000-0000-000000000001', "
                        + "'10000000-0000-0000-0000-000000000002', "
                        + "'10000000-0000-0000-0000-000000000041', 26000, '2021-01-01T00:00:00Z')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
