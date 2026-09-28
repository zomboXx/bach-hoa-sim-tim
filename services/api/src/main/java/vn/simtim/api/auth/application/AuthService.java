package vn.simtim.api.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.auth.domain.*;

@Service
public class AuthService {
    public record Grant(String accessToken, String tokenType, Instant expiresAt, SessionPrincipal session) {
        @Override public String toString() { return "Grant[redacted]"; }
    }
    private final AuthRepository repository;
    private final PasswordHasher passwords;
    private final Duration lifetime;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthService(AuthRepository repository, PasswordHasher passwords, Clock clock,
            @Value("${simtim.auth.session-lifetime:PT8H}") Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Session lifetime must be positive and at most one day");
        }
        this.repository = repository;
        this.passwords = passwords;
        this.clock = clock;
        this.lifetime = lifetime;
        this.dummyHash = passwords.hash(UUID.randomUUID().toString());
    }

    @Transactional
    public Grant login(String organizationCode, String storeCode, String username, String password) {
        if (!bounded(organizationCode, 40) || !bounded(storeCode, 40) || !bounded(username, 80)
                || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthFailure("INVALID_REQUEST", "Invalid login request");
        }
        var account = repository.findAccount(organizationCode.strip(), storeCode.strip(), username.strip());
        boolean matches = passwords.matches(password, account.map(AuthRepository.LoginAccount::passwordHash).orElse(dummyHash));
        if (!matches || account.isEmpty()) throw invalidCredentials();
        var principal = repository.principal(account.get().userId(), account.get().storeId())
                .filter(p -> !p.roles().isEmpty()).orElseThrow(AuthService::invalidCredentials);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expires = now.plus(lifetime);
        repository.createSession(UUID.randomUUID(), principal, digest(token), now, expires);
        return new Grant(token, "Bearer", expires, principal);
    }

    @Transactional(readOnly = true)
    public Optional<SessionPrincipal> authenticate(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return repository.resolveSession(digest(token), clock.instant()).filter(p -> !p.roles().isEmpty());
    }

    @Transactional
    public void logout(String token) { repository.revokeSession(digest(token), clock.instant()); }

    private static boolean bounded(String value, int max) {
        return value != null && !value.isBlank() && value.length() <= max;
    }
    private static AuthFailure invalidCredentials() {
        return new AuthFailure("INVALID_CREDENTIALS", "Invalid credentials or unavailable account");
    }
    private static byte[] digest(String token) {
        try { return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
