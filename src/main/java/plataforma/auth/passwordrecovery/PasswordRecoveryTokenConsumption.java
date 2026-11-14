package plataforma.auth.passwordrecovery;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Result of the atomic, single-use transition used by a password reset flow. */
public final class PasswordRecoveryTokenConsumption {
    private final PasswordRecoveryTokenStatus status;
    private final UUID userId;
    private final Instant consumedAt;

    private PasswordRecoveryTokenConsumption(
            PasswordRecoveryTokenStatus status,
            UUID userId,
            Instant consumedAt) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.userId = userId;
        this.consumedAt = consumedAt;
    }

    static PasswordRecoveryTokenConsumption consumed(UUID userId, Instant consumedAt) {
        return new PasswordRecoveryTokenConsumption(
                PasswordRecoveryTokenStatus.CONSUMED,
                Objects.requireNonNull(userId, "userId must not be null"),
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

    public UUID userId() {
        if (!isConsumed()) {
            throw new IllegalStateException("userId is available only after successful consumption");
        }
        return userId;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    @Override
    public String toString() {
        return "PasswordRecoveryTokenConsumption{status=" + status
                + ", consumedAt=" + consumedAt + ", userId=" + userId + "}";
    }
}
