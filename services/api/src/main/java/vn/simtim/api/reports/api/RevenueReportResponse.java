package vn.simtim.api.reports.api;

public record RevenueReportResponse(
        long revenue,
        long invoiceCount
) {
}
