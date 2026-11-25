package plataforma.auth.passwordrecovery;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Durable adapter expected by the AUTH-10-DB migration. */
class JpaPasswordRecoveryTokenRepository implements PasswordRecoveryTokenRepository {

    private final PasswordRecoveryTokenJpaRepository repository;

    JpaPasswordRecoveryTokenRepository(PasswordRecoveryTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PasswordRecoveryTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public void save(PasswordRecoveryToken token) {
        repository.save(PasswordRecoveryTokenEntity.from(token));
    }

    @Override
    @Transactional
    public int revokeActiveByUserId(UUID userId, Instant revokedAt) {
        return repository.revokeActiveByUserId(userId, revokedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public long countIssuedSince(UUID userId, Instant since) {
        return repository.countByUserIdAndIssuedAtGreaterThanEqual(userId, since);
    }

    @Override
    @Transactional
    public Optional<PasswordRecoveryToken> consumeIfActive(String tokenHash, Instant consumedAt) {
        if (repository.consumeIfActive(tokenHash, consumedAt) == 0) {
            return Optional.empty();
        }
        return repository.findByTokenHash(tokenHash).map(PasswordRecoveryTokenEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean revokeIfActive(String tokenHash, Instant revokedAt) {
        return repository.revokeIfActive(tokenHash, revokedAt) > 0;
    }
}
