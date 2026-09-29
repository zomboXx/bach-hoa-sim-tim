package vn.simtim.api.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthRepository {
    record LoginAccount(UUID userId, UUID storeId, String passwordHash) {}
    Optional<LoginAccount> findAccount(String organizationCode, String storeCode, String username);
    Optional<SessionPrincipal> principal(UUID userId, UUID storeId);
    void createSession(UUID id, SessionPrincipal principal, byte[] tokenHash, Instant issuedAt, Instant expiresAt);
    Optional<SessionPrincipal> resolveSession(byte[] tokenHash, Instant now);
    void revokeSession(byte[] tokenHash, Instant now);
}
