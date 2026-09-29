package vn.simtim.api.catalog.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.catalog.application.UnitService;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService service;

    public UnitController(UnitService service) {
        this.service = service;
    }

    @GetMapping
    public List<UnitResponse> list(@RequestHeader("X-Organization-Id") UUID orgId) {
        return service.listByOrg(orgId).stream().map(UnitResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UnitResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                            @PathVariable UUID id) {
        return UnitResponse.from(service.getById(orgId, id));
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(@RequestHeader("X-Organization-Id") UUID orgId,
                                               @Valid @RequestBody UnitRequest req) {
        var created = service.create(orgId, req.code(), req.name(), req.precisionScale());
        return ResponseEntity
                .created(URI.create("/api/v1/units/" + created.id()))
                .body(UnitResponse.from(created));
    }

    @PutMapping("/{id}")
    public UnitResponse update(@RequestHeader("X-Organization-Id") UUID orgId,
                               @PathVariable UUID id,
                               @Valid @RequestBody UnitRequest req) {
        return UnitResponse.from(service.update(orgId, id, req.code(), req.name(), req.precisionScale()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Organization-Id") UUID orgId,
                                       @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
