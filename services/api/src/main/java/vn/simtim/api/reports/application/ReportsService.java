package vn.simtim.api.reports.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.simtim.api.reports.api.InventoryReportResponse;
import vn.simtim.api.reports.api.RevenueReportResponse;
import vn.simtim.api.reports.domain.ReportsRepository;

@Service
public class ReportsService {

    private final ReportsRepository repository;
    private final Clock clock;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public ReportsService(ReportsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public RevenueReportResponse getRevenue(UUID orgId, UUID storeId, LocalDate fromDate, LocalDate toDate) {
        var from = fromDate.atStartOfDay(VN_ZONE).toOffsetDateTime();
        var to = toDate.atStartOfDay(VN_ZONE).toOffsetDateTime();
        return repository.getRevenue(orgId, storeId, from, to);
    }

    public InventoryReportResponse getInventory(UUID orgId, UUID storeId) {
        var now = OffsetDateTime.now(clock);
        var today = LocalDate.now(clock.withZone(VN_ZONE));
        var items = repository.getInventory(orgId, storeId, today);
        return new InventoryReportResponse(today, now, items);
    }
}
