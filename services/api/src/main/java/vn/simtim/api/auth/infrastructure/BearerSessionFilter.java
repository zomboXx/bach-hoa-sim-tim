package vn.simtim.api.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.simtim.api.auth.api.AuthErrors;
import vn.simtim.api.auth.application.AuthService;

public class BearerSessionFilter extends OncePerRequestFilter {
    private final AuthService service;
    private final ObjectMapper mapper;
    private final LoginRateLimiter limiter;
    public BearerSessionFilter(AuthService service, ObjectMapper mapper, LoginRateLimiter limiter) {
        this.service = service;
        this.mapper = mapper;
        this.limiter = limiter;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getMethod().equals("POST") && request.getServletPath().equals("/api/v1/auth/login")
                && !limiter.allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            writeError(response, 429, "RATE_LIMITED", "Too many login requests");
            return;
        }
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (java.util.Collections.list(request.getHeaders("Authorization")).size() != 1) {
                writeError(response, 401, "UNAUTHENTICATED", "Exactly one bearer session required");
                return;
            }
            var principal = header.regionMatches(true, 0, "Bearer ", 0, 7)
                    ? service.authenticate(header.substring(7))
                    : java.util.Optional.<vn.simtim.api.auth.domain.SessionPrincipal>empty();
            if (principal.isEmpty()) {
                writeError(response, 401, "UNAUTHENTICATED", "Valid bearer session required");
                return;
            }
            var p = principal.get();
            String org = request.getHeader("X-Organization-Id");
            String store = request.getHeader("X-Store-Id");
            if ((org != null && !org.equalsIgnoreCase(p.organizationId().toString()))
                    || (store != null && !store.equalsIgnoreCase(p.storeId().toString()))) {
                writeError(response, 403, "FORBIDDEN", "Session scope does not match request");
                return;
            }
            var authorities = p.permissions().stream().map(SimpleGrantedAuthority::new).toList();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p, null, authorities));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
    public void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), new AuthErrors.Error(code, message));
    }
}
