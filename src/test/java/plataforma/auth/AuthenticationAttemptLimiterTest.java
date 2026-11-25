package plataforma.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuthenticationAttemptLimiterTest {
    @Test
    void clearsFailureWindowAfterFifteenMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AuthenticationAttemptLimiter limiter = new AuthenticationAttemptLimiter(clock);

        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.checkAllowed("12345678901", "192.0.2.1");
            limiter.recordFailure("12345678901", "192.0.2.1");
        }

        assertThrows(
                AuthenticationRateLimitException.class,
                () -> limiter.checkAllowed("12345678901", "192.0.2.1")
        );

        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> limiter.checkAllowed("12345678901", "192.0.2.1"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;
        private final ZoneId zone;

        private MutableClock(Instant initial) {
            this(new AtomicReference<>(initial), ZoneId.of("UTC"));
        }

        private MutableClock(AtomicReference<Instant> current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        private void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }
    }
}
