package vn.simtim.api.auth.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.application.AuthService;
import vn.simtim.api.auth.domain.SessionPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record LoginRequest(String organizationCode, String storeCode, String username, String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public ResponseEntity<AuthService.Grant> login(@RequestBody LoginRequest request) {
        if (request == null) throw new vn.simtim.api.auth.domain.AuthFailure("INVALID_REQUEST", "Invalid login request");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(
                request.organizationCode(), request.storeCode(), request.username(), request.password()));
    }

    @GetMapping("/session")
    public ResponseEntity<SessionPrincipal> session(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body((SessionPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        service.logout(authorization.substring(7));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
