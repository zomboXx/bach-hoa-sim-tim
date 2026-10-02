package vn.simtim.api.inventory.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.inventory.application.ConfirmReceiptCommand;
import vn.simtim.api.inventory.application.GoodsReceiptService;

/**
 * HTTP adapter cho INV-01: POST/GET /api/v1/inventory/receipts.
 * Quyền: receipts.write (POST), receipts.read (GET) — kiểm tra trong filter.
 */
@RestController
@RequestMapping("/api/v1/inventory/receipts")
public class ReceiptController {

    private final GoodsReceiptService service;
    private final ObjectMapper objectMapper;

    public ReceiptController(GoodsReceiptService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /** POST /api/v1/inventory/receipts — xác nhận phiếu nhận hàng. */
    @PostMapping
    public ResponseEntity<ReceiptResponse> confirm(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody ReceiptRequest req,
            Authentication auth) {

        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        requirePermission(principal, "receipts.write");

        String payloadJson = serialize(req);
        var cmd = new ConfirmReceiptCommand(
                req.supplierId(), req.clientOperationId(),
                req.lines().stream().map(l -> new ConfirmReceiptCommand.LineCmd(
                        l.productId(), l.expectedQuantity(), l.deliveredQuantity(),
                        l.acceptedQuantity(), l.rejectedQuantity(), l.unitCost(),
                        l.supplierLotNumber(), l.expiryDate(), l.discrepancyReason()
                )).toList(),
                payloadJson);

        var receipt = service.confirm(cmd,
                principal.userId(), principal.organizationId(), principal.storeId(),
                idempotencyKey);

        return ResponseEntity
                .created(URI.create("/api/v1/inventory/receipts/" + receipt.id()))
                .body(ReceiptResponse.from(receipt));
    }

    /** GET /api/v1/inventory/receipts — danh sách phiếu theo store. */
    @GetMapping
    public List<ReceiptResponse> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication auth) {

        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        requirePermission(principal, "receipts.read");

        return service.list(principal.organizationId(), principal.storeId(), limit, offset)
                .stream().map(ReceiptResponse::from).toList();
    }

    /** GET /api/v1/inventory/receipts/{id} — chi tiết một phiếu. */
    @GetMapping("/{id}")
    public ReceiptResponse get(@PathVariable UUID id, Authentication auth) {
        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        requirePermission(principal, "receipts.read");
        return ReceiptResponse.from(service.getById(
                principal.organizationId(), principal.storeId(), id));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void requirePermission(SessionPrincipal p, String permission) {
        if (!p.permissions().contains(permission)) {
            throw new ReceiptForbiddenException("Thiếu quyền: " + permission);
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
