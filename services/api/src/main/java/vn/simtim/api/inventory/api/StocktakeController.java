package vn.simtim.api.inventory.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.inventory.application.StocktakeConflictException;
import vn.simtim.api.inventory.application.StocktakeService;
import vn.simtim.api.inventory.application.SubmitCountCommand;

/**
 * HTTP adapter cho SYN-02: API đồng bộ kiểm kê và xử lý xung đột.
 *
 * <p>Routes:
 * <ul>
 *   <li>POST   /api/v1/inventory/stocktakes           → mở hoặc lấy phiên OPEN</li>
 *   <li>GET    /api/v1/inventory/stocktakes/{id}      → chi tiết phiên (trong scope)</li>
 *   <li>POST   /api/v1/inventory/stocktakes/{id}/counts → gửi số đếm (idempotent)</li>
 * </ul>
 *
 * <p>Quyền:
 * <ul>
 *   <li>stocktakes.write — mở phiên và gửi số đếm (STOCK, MANAGER)</li>
 *   <li>stocktakes.read  — đọc phiên (STOCK, MANAGER)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/inventory/stocktakes")
public class StocktakeController {

    private final StocktakeService service;

    public StocktakeController(StocktakeService service) {
        this.service = service;
    }

    /**
     * POST /api/v1/inventory/stocktakes
     * Mở phiên OPEN mới cho actor/store hiện tại, hoặc trả phiên đang mở.
     */
    @PostMapping
    public ResponseEntity<StocktakeResponse> openSession(Authentication auth) {
        SessionPrincipal p = principal(auth);
        requirePermission(p, "stocktakes.write");

        var session = service.openOrGetSession(p.organizationId(), p.storeId(), p.userId());
        return ResponseEntity
                .created(URI.create("/api/v1/inventory/stocktakes/" + session.id()))
                .body(StocktakeResponse.from(session));
    }

    /**
     * GET /api/v1/inventory/stocktakes/{id}
     * Chi tiết một phiên trong phạm vi org/store của session.
     */
    @GetMapping("/{id}")
    public StocktakeResponse getSession(@PathVariable UUID id, Authentication auth) {
        SessionPrincipal p = principal(auth);
        requirePermission(p, "stocktakes.read");

        var session = service.getSession(p.organizationId(), p.storeId(), id);
        var lines   = service.getLines(p.organizationId(), p.storeId(), id);
        return StocktakeResponse.from(session, lines);
    }

    /**
     * POST /api/v1/inventory/stocktakes/{sessionId}/counts
     * Gửi số đếm thực tế cho một lô. Idempotent theo clientOperationId.
     * Trả 200 khi thành công (PENDING) hoặc 409 khi xung đột (CONFLICT).
     */
    @PostMapping("/{sessionId}/counts")
    public ResponseEntity<StocktakeResponse.LineDto> submitCount(
            @PathVariable UUID sessionId,
            @Valid @RequestBody StocktakeCountRequest req,
            Authentication auth) {

        SessionPrincipal p = principal(auth);
        requirePermission(p, "stocktakes.write");

        Instant countedAt = req.countedAt() != null ? req.countedAt() : Instant.now();
        var cmd = new SubmitCountCommand(
                req.clientOperationId(),
                req.batchId(),
                req.actualQuantity(),
                req.baseVersion(),
                req.note(),
                countedAt);

        var line = service.submitCount(
                p.organizationId(), p.storeId(), p.userId(), sessionId, cmd);

        return ResponseEntity.ok(StocktakeResponse.LineDto.from(line));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SessionPrincipal principal(Authentication auth) {
        return (SessionPrincipal) auth.getPrincipal();
    }

    private static void requirePermission(SessionPrincipal p, String permission) {
        if (!p.permissions().contains(permission)) {
            throw new StocktakeForbiddenException("Thiếu quyền: " + permission);
        }
    }
}
