package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Duration;
import java.util.Objects;

/** Technical policy supplied by the application that owns the recovery flow. */
public record PasswordRecoveryTokenPolicy(Duration lifetime, int entropyBytes) {

    public PasswordRecoveryTokenPolicy {
        Objects.requireNonNull(lifetime, "lifetime must not be null");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        if (entropyBytes < 32) {
            throw new IllegalArgumentException("entropyBytes must be at least 32");
        }
    }

    public static PasswordRecoveryTokenPolicy withLifetime(Duration lifetime) {
        return new PasswordRecoveryTokenPolicy(lifetime, 32);
    }
}
