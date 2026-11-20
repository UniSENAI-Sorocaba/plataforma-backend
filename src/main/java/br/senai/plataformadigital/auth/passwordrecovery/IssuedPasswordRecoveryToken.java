package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Instant;
import java.util.Objects;

/**
 * One-time handoff of the raw token to the delivery adapter.
 *
 * <p>Callers must deliver the value immediately and must not log or persist
 * this object. Its {@code toString()} is intentionally redacted.</p>
 */
public final class IssuedPasswordRecoveryToken {
    private final String rawToken;
    private final Instant expiresAt;

    IssuedPasswordRecoveryToken(String rawToken, Instant expiresAt) {
        this.rawToken = Objects.requireNonNull(rawToken, "rawToken must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public String rawToken() {
        return rawToken;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "IssuedPasswordRecoveryToken{expiresAt=" + expiresAt + ", rawToken=<redacted>}";
    }
}
