package vn.simtim.api.auth.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.simtim.api.auth.domain.*;

@Repository
public class JdbcAuthRepository implements AuthRepository {
    private static final String PRINCIPAL_SELECT = """
            select u.id, u.organization_id, s.id as store_id, u.full_name, u.training_enabled,
              array_remove(array_agg(distinct r.code),null) as roles,
              array_remove(array_agg(distinct p.code),null) as permissions
            from iam.users u join core.organizations o on o.id=u.organization_id
            join core.stores s on s.organization_id=u.organization_id
            left join iam.user_roles ur on ur.user_id=u.id and ur.store_id=s.id
            left join iam.roles r on r.id=ur.role_id
            left join iam.role_permissions rp on rp.role_id=ur.role_id
            left join iam.permissions p on p.id=rp.permission_id
            """;
    private static final String ACTIVE = " and u.status='ACTIVE' and s.status='ACTIVE' and o.status='ACTIVE' ";
    private static final String GROUP = " group by u.id,s.id ";
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
        return jdbc.query(PRINCIPAL_SELECT + " where u.id=? and s.id=? " + ACTIVE + GROUP,
                (rs, n) -> principalRow(rs), userId, storeId).stream().findFirst();
    }

    public void createSession(UUID id, SessionPrincipal p, byte[] hash, Instant issued, Instant expires) {
        jdbc.update("""
                insert into iam.auth_sessions(id,organization_id,user_id,store_id,token_hash,issued_at,expires_at)
                values(?,?,?,?,?,?,?)
                """, id, p.organizationId(), p.userId(), p.storeId(), hash, Timestamp.from(issued), Timestamp.from(expires));
    }

    public Optional<SessionPrincipal> resolveSession(byte[] hash, Instant now) {
        return session(hash, now).map(SessionRecord::principal);
    }

    @Override
    public Optional<SessionRecord> session(byte[] hash, Instant now) {
        String sql = PRINCIPAL_SELECT.replace("select u.id,", "select auth.expires_at, u.id,")
                + " join iam.auth_sessions auth on auth.user_id=u.id and auth.store_id=s.id "
                + " where auth.token_hash=? and auth.revoked_at is null and auth.expires_at>? and auth.issued_at<=? "
                + ACTIVE + GROUP + ",auth.expires_at";
        return jdbc.query(sql, (rs, n) -> new SessionRecord(principalRow(rs), rs.getTimestamp("expires_at").toInstant()),
                hash, Timestamp.from(now), Timestamp.from(now)).stream().findFirst();
    }

    private static SessionPrincipal principalRow(ResultSet rs) throws SQLException {
        return new SessionPrincipal(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                rs.getObject("store_id", UUID.class), rs.getString("full_name"), rs.getBoolean("training_enabled"),
                new HashSet<>(Arrays.asList((String[]) rs.getArray("roles").getArray())),
                new HashSet<>(Arrays.asList((String[]) rs.getArray("permissions").getArray())));
    }

    public void revokeSession(byte[] hash, Instant now) {
        jdbc.update("update iam.auth_sessions set revoked_at=? where token_hash=? and revoked_at is null",
                Timestamp.from(now), hash);
    }
}
