package vn.simtim.api.promotion.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.promotion.application.PromotionService;
import vn.simtim.api.promotion.domain.PromotionProduct;

/**
 * HTTP adapter cho PRO-01B — khuyến mãi cơ bản.
 *
 * <ul>
 *   <li>GET  /api/v1/promotions                              – danh sách (promotions.read)
 *   <li>GET  /api/v1/promotions/applicable?productId=&storeId=&at= – tra cứu điểm bán (promotions.read)
 *   <li>GET  /api/v1/promotions/{id}                         – chi tiết (promotions.read)
 *   <li>POST /api/v1/promotions                              – tạo (promotions.write)
 *   <li>PUT  /api/v1/promotions/{id}                         – cập nhật (promotions.write)
 *   <li>DELETE /api/v1/promotions/{id}                       – xóa (promotions.write)
 *   <li>GET  /api/v1/promotions/{id}/products               – sản phẩm trong phạm vi (promotions.read)
 *   <li>POST /api/v1/promotions/{id}/products               – thêm sản phẩm (promotions.write)
 *   <li>DELETE /api/v1/promotions/{id}/products/{productId} – xóa sản phẩm (promotions.write)
 * </ul>
 */
@RestController
@RequestMapping({"/api/v1/sales/promotions", "/api/v1/promotions"})
public class PromotionController {

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromotionResponse> list(@RequestHeader("X-Organization-Id") UUID orgId) {
        return service.listByOrg(orgId).stream()
                .map(PromotionResponse::from)
                .toList();
    }

    /**
     * Tra cứu khuyến mãi áp dụng tại điểm bán.
     * Tham số {@code at} dạng ISO-8601; mặc định là thời điểm hiện tại.
     */
    @GetMapping("/applicable")
    public List<PromotionResponse> applicable(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @RequestParam UUID productId,
            @RequestParam UUID storeId,
            @RequestParam(required = false) Instant at) {
        return service.findApplicable(orgId, storeId, productId, at).stream()
                .map(p -> PromotionResponse.from(p, service.listProducts(orgId, p.id())))
                .toList();
    }

    @GetMapping("/{id}")
    public PromotionResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                                 @PathVariable UUID id) {
        var promotion = service.getById(orgId, id);
        var products = service.listProducts(orgId, id);
        return PromotionResponse.from(promotion, products);
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @Valid @RequestBody PromotionRequest req) {
        var created = service.create(orgId, req.storeId(), req.code(), req.name(),
                req.discountType(), req.discountValue(), req.startsAt(), req.endsAt(), req.status());
        return ResponseEntity
                .created(URI.create("/api/v1/promotions/" + created.id()))
                .body(PromotionResponse.from(created));
    }

    @PutMapping("/{id}")
    public PromotionResponse update(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest req) {
        var updated = service.update(orgId, id, req.storeId(), req.code(), req.name(),
                req.discountType(), req.discountValue(), req.startsAt(), req.endsAt(), req.status());
        var products = service.listProducts(orgId, id);
        return PromotionResponse.from(updated, products);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    // ===== Product scope endpoints =====

    @GetMapping("/{id}/products")
    public List<Map<String, UUID>> listProducts(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id) {
        return service.listProducts(orgId, id).stream()
                .map(pp -> Map.of("promotionId", pp.promotionId(), "productId", pp.productId()))
                .toList();
    }

    @PostMapping("/{id}/products")
    public ResponseEntity<Void> addProduct(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        UUID productId = body.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }
        service.addProduct(orgId, id, productId);
        return ResponseEntity
                .created(URI.create("/api/v1/promotions/" + id + "/products/" + productId))
                .build();
    }

    @DeleteMapping("/{id}/products/{productId}")
    public ResponseEntity<Void> removeProduct(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @PathVariable UUID id,
            @PathVariable UUID productId) {
        service.removeProduct(orgId, id, productId);
        return ResponseEntity.noContent().build();
    }
}
