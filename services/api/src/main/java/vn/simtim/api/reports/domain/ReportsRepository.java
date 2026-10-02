package vn.simtim.api.reports.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import vn.simtim.api.reports.api.InventoryBalanceDto;
import vn.simtim.api.reports.api.RevenueReportResponse;

public interface ReportsRepository {
    RevenueReportResponse getRevenue(UUID orgId, UUID storeId, OffsetDateTime from, OffsetDateTime to);
    List<InventoryBalanceDto> getInventory(UUID orgId, UUID storeId, LocalDate today);
}
