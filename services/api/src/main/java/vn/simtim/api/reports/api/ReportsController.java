package vn.simtim.api.reports.api;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.simtim.api.reports.application.ReportsService;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    private final ReportsService service;

    public ReportsController(ReportsService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenue(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @RequestHeader("X-Store-Id") UUID storeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from.isAfter(to) || from.isEqual(to)) {
            return ResponseEntity.badRequest().build();
        }

        var res = service.getRevenue(orgId, storeId, from, to);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventoryReportResponse> getInventory(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @RequestHeader("X-Store-Id") UUID storeId) {
        var res = service.getInventory(orgId, storeId);
        return ResponseEntity.ok(res);
    }
}
