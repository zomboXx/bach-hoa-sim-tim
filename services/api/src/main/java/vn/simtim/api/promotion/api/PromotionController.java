package vn.simtim.api.promotion.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.promotion.application.PromotionService;
import vn.simtim.api.promotion.domain.PromotionProduct;

/**
 * HTTP adapter cho PRO-01B — khuyến mãi cơ bản.
 *
 * <ul>
 *   <li>GET  /api/v1/promotions                         – danh sách của store trong session (promotions.read)
 *   <li>GET  /api/v1/promotions/applicable?productId=&at= – tra cứu điểm bán (promotions.read)
 *   <li>GET  /api/v1/promotions/{id}                    – chi tiết (promotions.read)
 *   <li>POST /api/v1/promotions                         – tạo (promotions.write)
 *   <li>PUT  /api/v1/promotions/{id}                    – cập nhật (promotions.write)
 *   <li>DELETE /api/v1/promotions/{id}                  – xóa (promotions.write)
 *   <li>GET  /api/v1/promotions/{id}/products           – sản phẩm trong phạm vi (promotions.read)
 *   <li>POST /api/v1/promotions/{id}/products           – thêm sản phẩm (promotions.write)
 *   <li>DELETE /api/v1/promotions/{id}/products/{productId} – xóa sản phẩm (promotions.write)
 * </ul>
 *
 * organizationId và storeId luôn lấy từ SessionPrincipal; client không được override.
 */
@RestController
@RequestMapping({"/api/v1/sales/promotions", "/api/v1/promotions"})
public class PromotionController {

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromotionResponse> list(@RequestHeader("X-Organization-Id") UUID orgId,
                                        Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        return service.listByStore(principal.organizationId(), principal.storeId()).stream()
                .map(PromotionResponse::from)
                .toList();
    }

    /**
     * Tra cứu khuyến mãi áp dụng tại điểm bán.
     * storeId lấy từ SessionPrincipal — client không được cung cấp storeId.
     * Tham số {@code at} dạng ISO-8601; mặc định là thời điểm hiện tại.
     */
    @GetMapping("/applicable")
    public List<PromotionResponse> applicable(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @RequestParam UUID productId,
            @RequestParam(required = false) Instant at,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        UUID storeId = principal.storeId();
        return service.findApplicable(principal.organizationId(), storeId, productId, at).stream()
                .map(p -> PromotionResponse.from(p,
                        service.listProducts(principal.organizationId(), storeId, p.id())))
                .toList();
    }

    @GetMapping("/{id}")
    public PromotionResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                                 @PathVariable UUID id,
                                 Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        var promotion = service.getById(principal.organizationId(), principal.storeId(), id);
        var products = service.listProducts(principal.organizationId(), principal.storeId(), id);
        return PromotionResponse.from(promotion, products);
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @Valid @RequestBody PromotionRequest req,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        var created = service.create(principal.organizationId(), principal.storeId(),
                req.code(), req.name(),
                req.discountType(), req.discountValue(), req.startsAt(), req.endsAt(), req.status());
        return ResponseEntity
                .created(URI.create("/api/v1/promotions/" + created.id()))
                .body(PromotionResponse.from(created));
    }

    @PutMapping("/{id}")
    public PromotionResponse update(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest req,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        var updated = service.update(principal.organizationId(), principal.storeId(), id,
                req.code(), req.name(),
                req.discountType(), req.discountValue(), req.startsAt(), req.endsAt(), req.status());
        var products = service.listProducts(principal.organizationId(), principal.storeId(), id);
        return PromotionResponse.from(updated, products);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id,
                                       Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        service.delete(principal.organizationId(), principal.storeId(), id);
        return ResponseEntity.noContent().build();
    }

    // ===== Product scope endpoints =====

    @GetMapping("/{id}/products")
    public List<Map<String, UUID>> listProducts(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        return service.listProducts(principal.organizationId(), principal.storeId(), id).stream()
                .map(pp -> Map.of("promotionId", pp.promotionId(), "productId", pp.productId()))
                .toList();
    }

    @PostMapping("/{id}/products")
    public ResponseEntity<Void> addProduct(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        UUID productId = body.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }
        service.addProduct(principal.organizationId(), principal.storeId(), id, productId);
        return ResponseEntity
                .created(URI.create("/api/v1/promotions/" + id + "/products/" + productId))
                .build();
    }

    @DeleteMapping("/{id}/products/{productId}")
    public ResponseEntity<Void> removeProduct(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @PathVariable UUID productId,
            Authentication auth) {
        SessionPrincipal principal = principal(auth);
        requireSameOrg(principal, orgId);
        service.removeProduct(principal.organizationId(), principal.storeId(), id, productId);
        return ResponseEntity.noContent().build();
    }

    // ===== Private helpers =====

    private static SessionPrincipal principal(Authentication auth) {
        return (SessionPrincipal) auth.getPrincipal();
    }

    /**
     * Đảm bảo session thuộc đúng organization được yêu cầu.
     * Header X-Organization-Id vẫn được chấp nhận (tương thích BE-03 transition)
     * nhưng phải khớp với principal; không khớp → 403.
     */
    private static void requireSameOrg(SessionPrincipal principal, UUID requestedOrgId) {
        if (!principal.organizationId().equals(requestedOrgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "X-Organization-Id không khớp với phiên làm việc");
        }
    }
}
