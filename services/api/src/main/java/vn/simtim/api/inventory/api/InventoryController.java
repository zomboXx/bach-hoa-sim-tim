package vn.simtim.api.inventory.api;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.inventory.application.InventoryReadService;

/** HTTP adapter chỉ đọc cho tồn sản phẩm, lô và biến động. */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryReadService service;

    public InventoryController(InventoryReadService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public InventoryPageResponse<InventoryProductResponse> products(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return InventoryPageResponse.from(
                service.products(principal.organizationId(), principal.storeId(),
                        productId, status, page, size),
                InventoryProductResponse::from);
    }

    @GetMapping("/batches")
    public InventoryPageResponse<InventoryBatchResponse> batches(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String expiryStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return InventoryPageResponse.from(
                service.batches(principal.organizationId(), principal.storeId(),
                        productId, status, expiryStatus, page, size),
                InventoryBatchResponse::from);
    }

    @GetMapping("/movements")
    public InventoryPageResponse<InventoryMovementResponse> movements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false, name = "type") String movementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return InventoryPageResponse.from(
                service.movements(principal.organizationId(), principal.storeId(),
                        productId, batchId, movementType, page, size),
                InventoryMovementResponse::from);
    }

    private SessionPrincipal principal(Authentication authentication) {
        return (SessionPrincipal) authentication.getPrincipal();
    }
}
