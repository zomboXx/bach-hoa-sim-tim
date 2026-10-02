package vn.simtim.api.reports.api;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.reports.application.ReportsService;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    public record ErrorResponse(String code, String message) {}

    private final ReportsService service;

    public ReportsController(ReportsService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            Authentication authentication,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId,
            @RequestHeader(value = "X-Store-Id", required = false) UUID headerStoreId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        UUID orgId = headerOrgId;
        UUID storeId = headerStoreId;
        if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal) {
            orgId = principal.organizationId();
            storeId = principal.storeId();
        }

        if (orgId == null || storeId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "Organization and store scope required"));
        }

        if (from.isAfter(to) || from.isEqual(to)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "Từ ngày phải trước đến ngày"));
        }

        var res = service.getRevenue(orgId, storeId, from, to);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(
            Authentication authentication,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId,
            @RequestHeader(value = "X-Store-Id", required = false) UUID headerStoreId) {

        UUID orgId = headerOrgId;
        UUID storeId = headerStoreId;
        if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal) {
            orgId = principal.organizationId();
            storeId = principal.storeId();
        }

        if (orgId == null || storeId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "Organization and store scope required"));
        }

        var res = service.getInventory(orgId, storeId);
        return ResponseEntity.ok(res);
    }
}
