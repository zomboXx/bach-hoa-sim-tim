package vn.simtim.api.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.catalog.application.ProductService;

/**
 * HTTP adapter cho sản phẩm.
 * Hỗ trợ tìm kiếm theo ?sku=, ?barcode=, ?name= qua GET /api/v1/products/search.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestHeader("X-Organization-Id") UUID orgId) {
        return service.listByOrg(orgId).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestHeader("X-Organization-Id") UUID orgId,
                                        @RequestParam(required = false) String sku,
                                        @RequestParam(required = false) String barcode,
                                        @RequestParam(required = false) String name) {
        return service.search(orgId, sku, barcode, name).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                               @PathVariable UUID id) {
        return ProductResponse.from(service.getById(orgId, id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestHeader("X-Organization-Id") UUID orgId,
                                                  @Valid @RequestBody ProductRequest req) {
        var created = service.create(orgId, req.categoryId(), req.baseUnitId(),
                req.sku(), req.name(), req.tracksExpiry(),
                req.status() != null ? req.status() : "ACTIVE");
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + created.id()))
                .body(ProductResponse.from(created));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@RequestHeader("X-Organization-Id") UUID orgId,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(orgId, id, req.categoryId(), req.baseUnitId(),
                req.sku(), req.name(), req.tracksExpiry(),
                req.status() != null ? req.status() : "ACTIVE"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/barcodes")
    public List<ProductBarcodeResponse> listBarcodes(@RequestHeader("X-Organization-Id") UUID orgId,
                                                     @PathVariable UUID id) {
        return service.listBarcodes(orgId, id).stream().map(ProductBarcodeResponse::from).toList();
    }

    @PostMapping("/{id}/barcodes")
    public ResponseEntity<ProductBarcodeResponse> addBarcode(@RequestHeader("X-Organization-Id") UUID orgId,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody ProductBarcodeRequest req) {
        var created = service.addBarcode(orgId, id, req.barcode(), req.isPrimary());
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + id + "/barcodes/" + created.id()))
                .body(ProductBarcodeResponse.from(created));
    }

    @DeleteMapping("/{id}/barcodes/{barcodeId}")
    public ResponseEntity<Void> deleteBarcode(@RequestHeader("X-Organization-Id") UUID orgId,
                                              @PathVariable UUID id,
                                              @PathVariable UUID barcodeId) {
        service.deleteBarcode(orgId, id, barcodeId);
        return ResponseEntity.noContent().build();
    }
}
