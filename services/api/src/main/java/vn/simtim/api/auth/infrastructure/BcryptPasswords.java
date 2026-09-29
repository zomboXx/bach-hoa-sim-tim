package vn.simtim.api.auth.infrastructure;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import vn.simtim.api.auth.domain.PasswordHasher;

@Component
public class BcryptPasswords implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    public String hash(String password) { return encoder.encode(password); }
    public boolean matches(String password, String hash) {
        return hash != null && hash.startsWith("$2") && encoder.matches(password, hash);
    }
}
