package vn.simtim.api.training.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.training.application.TrainingService;
import vn.simtim.api.training.domain.TrainingFailure;

@RestController
@RequestMapping("/api/v1/training")
public class TrainingController {
    private final TrainingService service;

    public TrainingController(TrainingService service) { this.service = service; }

    private SessionPrincipal principal(Authentication authentication) {
        return (SessionPrincipal) authentication.getPrincipal();
    }

    @GetMapping("/scenarios")
    public Object scenarios(Authentication auth, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return service.scenarios(principal(auth));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> start(@RequestHeader("Idempotency-Key") UUID key,
            @RequestBody JsonNode body, Authentication auth) {
        var result = service.start(principal(auth), key, body);
        return ResponseEntity.created(URI.create("/api/v1/training/sessions/" + result.get("id")))
                .header("Cache-Control", "no-store").body(result);
    }

    @PostMapping("/sessions/{id}/actions")
    public ResponseEntity<Map<String, Object>> action(@PathVariable UUID id,
            @RequestBody JsonNode body, Authentication auth) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.action(principal(auth), id, body));
    }

    @PostMapping("/sessions/{id}/abandon")
    public ResponseEntity<Map<String, Object>> abandon(@PathVariable UUID id,
            @RequestBody(required = false) JsonNode body, Authentication auth) {
        if (body != null && (!body.isObject() || !body.isEmpty()))
            throw new TrainingFailure(400, "INVALID_REQUEST", "Abandon does not accept client state");
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.abandon(principal(auth), id));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> session(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.session(principal(auth), id));
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> results(
            @RequestParam(defaultValue = "SELF") String scope,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(service.results(principal(auth), scope, learnerId, page, size));
    }
}
