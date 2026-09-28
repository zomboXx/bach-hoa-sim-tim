package vn.simtim.api.auth.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.auth.domain.*;

@Repository
public class JdbcAuthRepository implements AuthRepository {
    private final JdbcTemplate jdbc;
    public JdbcAuthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<LoginAccount> findAccount(String organizationCode, String storeCode, String username) {
        return jdbc.query("""
                select u.id, s.id as store_id, u.password_hash
                from iam.users u join core.organizations o on o.id=u.organization_id
                join core.stores s on s.organization_id=o.id
                where o.code=? and s.code=? and lower(u.username)=lower(?)
                  and u.status='ACTIVE' and s.status='ACTIVE' and o.status='ACTIVE'
                """, (rs, n) -> new LoginAccount(rs.getObject("id", UUID.class),
                        rs.getObject("store_id", UUID.class), rs.getString("password_hash")),
                organizationCode, storeCode, username).stream().findFirst();
    }

    public Optional<SessionPrincipal> principal(UUID userId, UUID storeId) {
        var users = jdbc.query("""
                select u.id, u.organization_id, s.id as store_id, u.full_name, u.training_enabled
                from iam.users u join core.stores s on s.organization_id=u.organization_id
                join core.organizations o on o.id=u.organization_id
                where u.id=? and s.id=? and u.status='ACTIVE' and s.status='ACTIVE' and o.status='ACTIVE'
                """, (rs, n) -> new SessionPrincipal(rs.getObject("id", UUID.class),
                        rs.getObject("organization_id", UUID.class), rs.getObject("store_id", UUID.class),
                        rs.getString("full_name"), rs.getBoolean("training_enabled"),
                        new HashSet<>(jdbc.queryForList("""
                            select r.code from iam.user_roles ur join iam.roles r on r.id=ur.role_id
                            where ur.user_id=? and ur.store_id=?
                            """, String.class, userId, storeId)),
                        new HashSet<>(jdbc.queryForList("""
                            select distinct p.code from iam.user_roles ur
                            join iam.role_permissions rp on rp.role_id=ur.role_id
                            join iam.permissions p on p.id=rp.permission_id
                            where ur.user_id=? and ur.store_id=?
                            """, String.class, userId, storeId))), userId, storeId);
        return users.stream().findFirst();
    }

    public void createSession(UUID id, SessionPrincipal p, byte[] hash, Instant issued, Instant expires) {
        jdbc.update("""
                insert into iam.auth_sessions(id,organization_id,user_id,store_id,token_hash,issued_at,expires_at)
                values(?,?,?,?,?,?,?)
                """, id, p.organizationId(), p.userId(), p.storeId(), hash, Timestamp.from(issued), Timestamp.from(expires));
    }

    public Optional<SessionPrincipal> resolveSession(byte[] hash, Instant now) {
        return jdbc.query("""
                select user_id,store_id from iam.auth_sessions
                where token_hash=? and revoked_at is null and expires_at>? and issued_at<=?
                """, (rs, n) -> new UUID[]{rs.getObject("user_id", UUID.class), rs.getObject("store_id", UUID.class)},
                hash, Timestamp.from(now), Timestamp.from(now)).stream().findFirst()
                .flatMap(ids -> principal(ids[0], ids[1]));
    }

    public void revokeSession(byte[] hash, Instant now) {
        jdbc.update("update iam.auth_sessions set revoked_at=? where token_hash=? and revoked_at is null",
                Timestamp.from(now), hash);
    }
}
