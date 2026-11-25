package plataforma.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public final class AuthenticationAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final int MAX_TRACKED_IDENTIFIERS = 100_000;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Clock clock;
    private final SecretKeySpec fingerprintKey;
    private final Map<String, FailureWindow> failures = new ConcurrentHashMap<>();
    private final AtomicLong cleanupAfter = new AtomicLong();

    public AuthenticationAttemptLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        this.fingerprintKey = new SecretKeySpec(key, HMAC_ALGORITHM);
    }

    public void checkAllowed(String cpf, String remoteAddress) {
        long now = clock.millis();
        cleanupExpired(now);
        String cpfKey = fingerprint("cpf", cpf);
        String addressKey = fingerprint("ip", remoteAddress);

        if (isBlocked(cpfKey, now) || isBlocked(addressKey, now)
                || exceedsCapacity(cpfKey) || exceedsCapacity(addressKey)) {
            throw new AuthenticationRateLimitException();
        }
    }

    public void recordFailure(String cpf, String remoteAddress) {
        long now = clock.millis();
        increment(fingerprint("cpf", cpf), now);
        increment(fingerprint("ip", remoteAddress), now);
        cleanupExpired(now);
    }

    public void recordSuccess(String cpf) {
        failures.remove(fingerprint("cpf", cpf));
    }

    private boolean isBlocked(String key, long now) {
        FailureWindow window = failures.get(key);
        return window != null && !window.isExpired(now) && window.count() >= MAX_FAILURES;
    }

    private boolean exceedsCapacity(String key) {
        return failures.size() >= MAX_TRACKED_IDENTIFIERS && !failures.containsKey(key);
    }

    private void increment(String key, long now) {
        failures.compute(key, (ignored, current) -> current == null || current.isExpired(now)
                ? new FailureWindow(now, 1)
                : new FailureWindow(current.startedAt(), Math.min(current.count() + 1, MAX_FAILURES)));
    }

    private void cleanupExpired(long now) {
        long nextCleanup = cleanupAfter.get();
        if (now < nextCleanup || !cleanupAfter.compareAndSet(nextCleanup, now + CLEANUP_INTERVAL_MILLIS)) {
            return;
        }
        failures.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String fingerprint(String dimension, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(dimension + " must not be blank");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(fingerprintKey);
            byte[] fingerprint = mac.doFinal((dimension + ":" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(fingerprint);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to fingerprint authentication limiter key", exception);
        }
    }

    private record FailureWindow(long startedAt, int count) {
        boolean isExpired(long now) {
            return now - startedAt >= WINDOW_MILLIS;
        }
    }
}
