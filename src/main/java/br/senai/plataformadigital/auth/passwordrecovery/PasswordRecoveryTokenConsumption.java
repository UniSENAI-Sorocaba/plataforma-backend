package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Instant;
import java.util.Objects;

/** Result of the atomic, single-use transition used by a password reset flow. */
public final class PasswordRecoveryTokenConsumption {
    private final PasswordRecoveryTokenStatus status;
    private final String subject;
    private final Instant consumedAt;

    private PasswordRecoveryTokenConsumption(
            PasswordRecoveryTokenStatus status,
            String subject,
            Instant consumedAt) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.subject = subject;
        this.consumedAt = consumedAt;
    }

    static PasswordRecoveryTokenConsumption consumed(String subject, Instant consumedAt) {
        return new PasswordRecoveryTokenConsumption(
                PasswordRecoveryTokenStatus.CONSUMED,
                Objects.requireNonNull(subject, "subject must not be null"),
                Objects.requireNonNull(consumedAt, "consumedAt must not be null"));
    }

    static PasswordRecoveryTokenConsumption rejected(PasswordRecoveryTokenStatus status) {
        if (status == PasswordRecoveryTokenStatus.VALID || status == PasswordRecoveryTokenStatus.CONSUMED) {
            throw new IllegalArgumentException("a rejected token cannot have a successful status");
        }
        return new PasswordRecoveryTokenConsumption(status, null, null);
    }

    public PasswordRecoveryTokenStatus status() {
        return status;
    }

    public boolean isConsumed() {
        return status == PasswordRecoveryTokenStatus.CONSUMED;
    }

    public String subject() {
        if (!isConsumed()) {
            throw new IllegalStateException("subject is available only after successful consumption");
        }
        return subject;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    @Override
    public String toString() {
        return "PasswordRecoveryTokenConsumption{status=" + status
                + ", consumedAt=" + consumedAt + ", subject=<redacted>}";
    }
}
