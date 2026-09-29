package vn.simtim.api.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.catalog.application.SupplierService;

/** HTTP adapter cho nhà cung cấp. */
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> list(@RequestHeader("X-Organization-Id") UUID orgId) {
        return service.listByOrg(orgId).stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                                @PathVariable UUID id) {
        return SupplierResponse.from(service.getById(orgId, id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@RequestHeader("X-Organization-Id") UUID orgId,
                                                   @Valid @RequestBody SupplierRequest req) {
        var created = service.create(orgId, req.code(), req.name(), req.phone(), req.email(),
                req.status() != null ? req.status() : "ACTIVE");
        return ResponseEntity
                .created(URI.create("/api/v1/suppliers/" + created.id()))
                .body(SupplierResponse.from(created));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@RequestHeader("X-Organization-Id") UUID orgId,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody SupplierRequest req) {
        return SupplierResponse.from(service.update(orgId, id, req.code(), req.name(),
                req.phone(), req.email(), req.status() != null ? req.status() : "ACTIVE"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
