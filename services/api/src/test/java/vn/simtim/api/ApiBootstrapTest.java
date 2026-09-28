package vn.simtim.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("demo")
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
                "select count(*) from flyway_schema_history where script = 'R__demo_seed.sql' and success",
                Integer.class)).isEqualTo(1);
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
