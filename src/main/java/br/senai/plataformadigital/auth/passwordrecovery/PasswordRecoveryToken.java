package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted representation of a recovery token.
 *
 * <p>The raw token is deliberately absent. Only its SHA-256 digest is kept in
 * this object, so a persistence failure cannot disclose a usable credential.</p>
 */
public final class PasswordRecoveryToken {
    private final UUID id;
    private final String subject;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant usedAt;
    private final Instant revokedAt;

    private PasswordRecoveryToken(
            UUID id,
            String subject,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        if (usedAt != null && usedAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("usedAt must not be before issuedAt");
        }
        if (revokedAt != null && revokedAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("revokedAt must not be before issuedAt");
        }
        if (usedAt != null && revokedAt != null) {
            throw new IllegalArgumentException("a token cannot be used and revoked");
        }
    }

    public static PasswordRecoveryToken issued(
            UUID id,
            String subject,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new PasswordRecoveryToken(id, subject, tokenHash, issuedAt, expiresAt, null, null);
    }

    public UUID id() {
        return id;
    }

    public String subject() {
        return subject;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public boolean isActiveAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }

    PasswordRecoveryToken markUsedAt(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        return new PasswordRecoveryToken(id, subject, tokenHash, issuedAt, expiresAt, timestamp, null);
    }

    PasswordRecoveryToken markRevokedAt(Instant timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        return new PasswordRecoveryToken(id, subject, tokenHash, issuedAt, expiresAt, null, timestamp);
    }

    @Override
    public String toString() {
        return "PasswordRecoveryToken{id=" + id + ", issuedAt=" + issuedAt
                + ", expiresAt=" + expiresAt + ", used=" + (usedAt != null)
                + ", revoked=" + (revokedAt != null) + "}";
    }
}
