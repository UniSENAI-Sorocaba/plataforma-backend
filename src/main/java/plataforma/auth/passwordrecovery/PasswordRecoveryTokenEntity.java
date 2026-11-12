package plataforma.auth.passwordrecovery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA adapter model; the database migration is owned by AUTH-10-DB. */
@Entity
@Table(schema = "auth", name = "password_recovery_token", indexes = {
        @Index(name = "idx_password_recovery_token_user_issued", columnList = "user_id, issued_at")
})
class PasswordRecoveryTokenEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected PasswordRecoveryTokenEntity() {
        // JPA constructor
    }

    private PasswordRecoveryTokenEntity(PasswordRecoveryToken token) {
        this.id = token.id();
        this.userId = token.userId();
        this.tokenHash = token.tokenHash();
        this.issuedAt = token.issuedAt();
        this.expiresAt = token.expiresAt();
        this.usedAt = token.usedAt();
        this.revokedAt = token.revokedAt();
    }

    static PasswordRecoveryTokenEntity from(PasswordRecoveryToken token) {
        return new PasswordRecoveryTokenEntity(token);
    }

    PasswordRecoveryToken toDomain() {
        PasswordRecoveryToken token = PasswordRecoveryToken.issued(id, userId, tokenHash, issuedAt, expiresAt);
        if (usedAt != null) {
            return token.markUsedAt(usedAt);
        }
        if (revokedAt != null) {
            return token.markRevokedAt(revokedAt);
        }
        return token;
    }

    UUID getUserId() {
        return userId;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }
}
