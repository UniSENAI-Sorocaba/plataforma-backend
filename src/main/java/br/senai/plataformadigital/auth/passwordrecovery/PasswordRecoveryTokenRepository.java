package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Instant;
import java.util.Optional;

/** Persistence port for hashed password-recovery tokens. */
public interface PasswordRecoveryTokenRepository {

    Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash);

    void save(PasswordRecoveryToken token);

    int revokeActiveBySubject(String subject, Instant revokedAt);

    Optional<PasswordRecoveryToken> consumeIfActive(String tokenHash, Instant consumedAt);

    boolean revokeIfActive(String tokenHash, Instant revokedAt);
}
