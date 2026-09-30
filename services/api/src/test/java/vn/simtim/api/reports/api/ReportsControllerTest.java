package vn.simtim.api.reports.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import vn.simtim.api.auth.infrastructure.AuthSecurity;
import vn.simtim.api.reports.application.ReportsService;

@WebMvcTest(ReportsController.class)
@Import(AuthSecurity.class)
class ReportsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ReportsService service;

    @Test
    void shouldReturnRevenueReport() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(service.getRevenue(eq(orgId), eq(storeId), eq(from), eq(to)))
                .thenReturn(new RevenueReportResponse(1500000L, 5L));

        mvc.perform(get("/api/v1/reports/revenue")
                .header("X-Organization-Id", orgId.toString())
                .header("X-Store-Id", storeId.toString())
                .param("from", "2026-09-01")
                .param("to", "2026-09-30"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.revenue").value(1500000))
           .andExpect(jsonPath("$.invoiceCount").value(5));
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        mvc.perform(get("/api/v1/reports/revenue")
                .header("X-Organization-Id", orgId.toString())
                .header("X-Store-Id", storeId.toString())
                .param("from", "2026-09-30")
                .param("to", "2026-09-01"))
           .andExpect(status().isBadRequest());
    }
}
