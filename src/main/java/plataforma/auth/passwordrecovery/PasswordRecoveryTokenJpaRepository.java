package plataforma.auth.passwordrecovery;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PasswordRecoveryTokenJpaRepository extends JpaRepository<PasswordRecoveryTokenEntity, UUID> {

    Optional<PasswordRecoveryTokenEntity> findByTokenHash(String tokenHash);

    long countByUserIdAndIssuedAtGreaterThanEqual(UUID userId, Instant since);

    @Modifying
    @Query("""
            update PasswordRecoveryTokenEntity token
               set token.revokedAt = :revokedAt
             where token.userId = :userId
               and token.usedAt is null
               and token.revokedAt is null
               and token.expiresAt > :revokedAt
            """)
    int revokeActiveByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            update PasswordRecoveryTokenEntity token
               set token.usedAt = :consumedAt
             where token.tokenHash = :tokenHash
               and token.usedAt is null
               and token.revokedAt is null
               and token.expiresAt > :consumedAt
            """)
    int consumeIfActive(
            @Param("tokenHash") String tokenHash,
            @Param("consumedAt") Instant consumedAt);

    @Modifying
    @Query("""
            update PasswordRecoveryTokenEntity token
               set token.revokedAt = :revokedAt
             where token.tokenHash = :tokenHash
               and token.usedAt is null
               and token.revokedAt is null
               and token.expiresAt > :revokedAt
            """)
    int revokeIfActive(
            @Param("tokenHash") String tokenHash,
            @Param("revokedAt") Instant revokedAt);
}
