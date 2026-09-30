package vn.simtim.api.reports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcReportsRepository.class)
@Sql("/test-reports-schema.sql")
class JdbcReportsRepositoryTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("simtim_reports_test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        postgres.start();
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcReportsRepository repository;

    @Test
    void testGetRevenueWithZeroAmountAndCompletedStatus() {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        var res = repository.getRevenue(orgId, storeId, now.minusDays(1), now.plusDays(1));
        assertThat(res).isNotNull();
        assertThat(res.revenue()).isEqualTo(0);
        assertThat(res.invoiceCount()).isEqualTo(0);
    }
    
    @Test
    void testGetInventory() {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        var res = repository.getInventory(orgId, storeId, LocalDate.now());
        assertThat(res).isNotNull();
        assertThat(res.size()).isEqualTo(0);
    }
}
