package br.senai.plataformadigital.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

interface RefreshTokenRepository {
    void createSession(UUID sessionId, UUID userId, UUID familyId, Instant createdAt, Instant expiresAt);

    void save(RefreshTokenRecord token);

    Optional<RefreshTokenRecord> findByHash(String tokenHash);

    void replace(RefreshTokenRecord token);

    List<RefreshTokenRecord> findByFamily(UUID familyId);

    List<RefreshTokenRecord> findBySession(UUID sessionId);

    void revokeSession(UUID sessionId, Instant revokedAt);

    void revokeFamily(UUID familyId, Instant revokedAt);
}
