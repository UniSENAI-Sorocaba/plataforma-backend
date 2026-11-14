package plataforma.auth.passwordrecovery;

import java.time.Instant;

/**
 * Delivery port for e-mail or another approved channel.
 *
 * <p>The raw token is a credential. Implementations must not log it, persist
 * it, put it in a URL, or include it in telemetry.</p>
 */
@FunctionalInterface
public interface PasswordRecoveryTokenDelivery {
    void deliver(String normalizedSubject, String rawToken, Instant expiresAt);
}
