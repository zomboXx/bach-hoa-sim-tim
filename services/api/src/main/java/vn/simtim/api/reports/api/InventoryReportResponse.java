package vn.simtim.api.reports.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record InventoryReportResponse(
        LocalDate businessDate,
        OffsetDateTime asOf,
        List<InventoryBalanceDto> items
) {
}
