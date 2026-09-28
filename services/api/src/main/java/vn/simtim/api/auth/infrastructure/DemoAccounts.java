package vn.simtim.api.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.auth.domain.PasswordHasher;

/** Local demo bootstrap only. No credentials are loaded unless explicitly supplied. */
@Component
@Profile("demo")
public class DemoAccounts implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordHasher passwords;
    private final String password;
    public DemoAccounts(JdbcTemplate jdbc, PasswordHasher passwords,
            @Value("${SIMTIM_DEMO_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.isEmpty()) return;
        if (password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72 || password.length() < 12) {
            throw new IllegalArgumentException("Demo password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
        UUID org = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID store = UUID.fromString("10000000-0000-0000-0000-000000000002");
        int index = 101;
        for (String role : new String[]{"SALES", "STOCK", "MANAGER", "ADMIN"}) {
            UUID id = UUID.fromString("20000000-0000-0000-0000-000000000" + index++);
            jdbc.update("""
                    insert into iam.users(id,organization_id,username,password_hash,full_name,status)
                    values(?,?,?,?,?,'ACTIVE') on conflict do nothing
                    """, id, org, role.toLowerCase(java.util.Locale.ROOT), passwords.hash(password), "Demo " + role);
            jdbc.update("""
                    insert into iam.user_roles(organization_id,user_id,role_id,store_id,assigned_at,assigned_by)
                    select ?,u.id,r.id,?,now(),u.id from iam.users u join iam.roles r on r.organization_id=u.organization_id
                    where u.id=? and u.organization_id=? and r.code=? on conflict do nothing
                    """, org, store, id, org, role);
        }
    }
}
