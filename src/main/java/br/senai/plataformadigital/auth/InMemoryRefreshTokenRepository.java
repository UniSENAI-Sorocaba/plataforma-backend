package br.senai.plataformadigital.auth;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
    private final ConcurrentMap<String, RefreshTokenRecord> tokens = new ConcurrentHashMap<>();

    @Override
    public void createSession(UUID sessionId, UUID userId, UUID familyId, Instant createdAt, Instant expiresAt) {
        // The in-memory adapter keeps token lifecycle state; session persistence is supplied by DB-005 in production.
    }

    @Override
    public void save(RefreshTokenRecord token) {
        tokens.put(token.tokenHash(), token);
    }

    @Override
    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        return Optional.ofNullable(tokens.get(tokenHash));
    }

    @Override
    public void replace(RefreshTokenRecord token) {
        tokens.put(token.tokenHash(), token);
    }

    @Override
    public List<RefreshTokenRecord> findByFamily(UUID familyId) {
        return new ArrayList<>(tokens.values()).stream()
                .filter(token -> token.familyId().equals(familyId))
                .toList();
    }

    @Override
    public List<RefreshTokenRecord> findBySession(UUID sessionId) {
        return new ArrayList<>(tokens.values()).stream()
                .filter(token -> token.sessionId().equals(sessionId))
                .toList();
    }

    @Override
    public void revokeSession(UUID sessionId, Instant revokedAt) {
        findBySession(sessionId).forEach(token -> replace(token.revokedAt(revokedAt)));
    }

    @Override
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        findByFamily(familyId).forEach(token -> replace(token.revokedAt(revokedAt)));
    }
}
