package plataforma.auth.passwordrecovery;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for hashed password-recovery tokens. */
public interface PasswordRecoveryTokenRepository {

    Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash);

    void save(PasswordRecoveryToken token);

    int revokeActiveByUserId(UUID userId, Instant revokedAt);

    long countIssuedSince(UUID userId, Instant since);

    Optional<PasswordRecoveryToken> consumeIfActive(String tokenHash, Instant consumedAt);

    boolean revokeIfActive(String tokenHash, Instant revokedAt);
}
