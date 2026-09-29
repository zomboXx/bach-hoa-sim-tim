package vn.simtim.api.auth.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bounded per-instance limit, using socket peer address rather than untrusted forwarded headers. */
@Component
public class LoginRateLimiter {
    private record Window(Instant until, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private final int limit;
    public LoginRateLimiter(Clock clock, @Value("${simtim.auth.login-requests-per-minute:20}") int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid login rate limit");
        this.clock = clock;
        this.limit = limit;
    }
    public synchronized boolean allow(String address) {
        Instant now = clock.instant();
        Window window = windows.get(address);
        if (window == null || !window.until().isAfter(now)) {
            if (windows.size() >= 4096) {
                windows.entrySet().removeIf(entry -> !entry.getValue().until().isAfter(now));
                if (windows.size() >= 4096 && window == null) return false;
            }
            window = new Window(now.plusSeconds(60), 0);
        }
        if (window.count() >= limit) return false;
        windows.put(address, new Window(window.until(), window.count() + 1));
        return true;
    }
}
