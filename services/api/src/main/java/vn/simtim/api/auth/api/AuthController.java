package vn.simtim.api.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.application.AuthService;
import vn.simtim.api.auth.domain.SessionPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String RESTORE_COOKIE = "__Secure-simtim-restore";
    private static final String RESTORE_PATH = "/api/v1/auth/restore";
    public record LoginRequest(String organizationCode, String storeCode, String username, String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if (request == null) throw new vn.simtim.api.auth.domain.AuthFailure("INVALID_REQUEST", "Invalid login request");
        boolean browser = http.getHeader("X-SimTim-Browser") != null;
        if (browser && !browserRequest(http)) return forbidden();
        var grant = service.login(request.organizationCode(), request.storeCode(), request.username(), request.password());
        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (browser) response.header(HttpHeaders.SET_COOKIE,
                cookie(grant.accessToken(), Duration.between(Instant.now(), grant.expiresAt())));
        return response.body(grant);
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(HttpServletRequest http) {
        if (!browserRequest(http)) return forbidden();
        String token = null;
        int matches = 0;
        if (http.getCookies() != null) {
            for (var candidate : http.getCookies()) {
                if (RESTORE_COOKIE.equals(candidate.getName())) {
                    token = candidate.getValue();
                    matches++;
                }
            }
        }
        var grant = service.restore(matches == 1 ? token : null);
        if (grant.isEmpty()) {
            return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO))
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    .body(new AuthErrors.Error("UNAUTHENTICATED", "Valid browser session required"));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(grant.get());
    }

    @GetMapping("/session")
    public ResponseEntity<SessionPrincipal> session(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body((SessionPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        service.logout(authorization.substring(7));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private static boolean browserRequest(HttpServletRequest request) {
        // This non-simple header requires CORS preflight cross-origin; CORS stays disabled.
        // Fetch Metadata additionally excludes sibling origins considered same-site by cookies.
        String site = request.getHeader("Sec-Fetch-Site");
        return "1".equals(request.getHeader("X-SimTim-Browser")) && (site == null || site.equals("same-origin"));
    }

    private static ResponseEntity<AuthErrors.Error> forbidden() {
        return ResponseEntity.status(403).cacheControl(CacheControl.noStore())
                .body(new AuthErrors.Error("FORBIDDEN", "Same-origin browser request required"));
    }

    private static String cookie(String token, Duration age) {
        return ResponseCookie.from(RESTORE_COOKIE, token).httpOnly(true).secure(true).sameSite("Strict")
                .path(RESTORE_PATH).maxAge(age.isNegative() ? Duration.ZERO : age).build().toString();
    }
}
