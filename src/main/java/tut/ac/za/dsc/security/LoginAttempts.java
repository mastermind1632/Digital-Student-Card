package tut.ac.za.dsc.security;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing: after 5 failed sign-ins for one account, or 30 from one network address
 * (higher, because a whole campus can share an address), sign-in is refused for 10 minutes.
 */
@Component
public class LoginAttempts {

    private static final int MAX_PER_ACCOUNT = 5;
    private static final int MAX_PER_ADDRESS = 30;
    private static final long LOCK_MS = 10 * 60 * 1000L;
    private final ConcurrentHashMap<String, long[]> failures = new ConcurrentHashMap<>();

    /** Minutes left on a lock, or 0 when sign-in may be attempted. */
    public long minutesLocked(String account, String address) {
        return Math.max(left("a:" + account, MAX_PER_ACCOUNT), left("ip:" + address, MAX_PER_ADDRESS));
    }

    public void failed(String account, String address) {
        bump("a:" + account);
        bump("ip:" + address);
        if (failures.size() > 10_000) failures.clear();
    }

    public void succeeded(String account, String address) {
        failures.remove("a:" + account);
        failures.remove("ip:" + address);
    }

    private long left(String key, int max) {
        long[] f = failures.get(key);
        if (f == null || f[0] < max) return 0;
        long ms = f[1] + LOCK_MS - System.currentTimeMillis();
        if (ms <= 0) {
            failures.remove(key);
            return 0;
        }
        return (ms + 59_999) / 60_000;
    }

    private void bump(String key) {
        long now = System.currentTimeMillis();
        failures.compute(key, (k, f) -> {
            if (f == null || now - f[1] > LOCK_MS) return new long[]{1, now};
            f[0]++;
            f[1] = now;
            return f;
        });
    }
}
