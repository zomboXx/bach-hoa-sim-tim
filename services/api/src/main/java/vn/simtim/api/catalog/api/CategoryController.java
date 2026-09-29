package vn.simtim.api.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.catalog.application.CategoryService;

/**
 * HTTP adapter cho danh mục sản phẩm.
 * organizationId truyền qua header X-Organization-Id (tạm thời; BE-02 sẽ lấy từ JWT).
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestHeader("X-Organization-Id") UUID orgId) {
        return service.listByOrg(orgId).stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                                @PathVariable UUID id) {
        return CategoryResponse.from(service.getById(orgId, id));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestHeader("X-Organization-Id") UUID orgId,
                                                   @Valid @RequestBody CategoryRequest req) {
        var created = service.create(orgId, req.parentId(), req.code(), req.name(),
                req.status() != null ? req.status() : "ACTIVE");
        return ResponseEntity
                .created(URI.create("/api/v1/categories/" + created.id()))
                .body(CategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@RequestHeader("X-Organization-Id") UUID orgId,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody CategoryRequest req) {
        return CategoryResponse.from(service.update(orgId, id, req.parentId(), req.code(),
                req.name(), req.status() != null ? req.status() : "ACTIVE"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
