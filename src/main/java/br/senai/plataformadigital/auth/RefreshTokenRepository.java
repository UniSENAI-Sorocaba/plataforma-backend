package br.senai.plataformadigital.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenRepository {
    void save(RefreshTokenRecord token);

    Optional<RefreshTokenRecord> findByHash(String tokenHash);

    void replace(RefreshTokenRecord token);

    List<RefreshTokenRecord> findByFamily(UUID familyId);

    List<RefreshTokenRecord> findBySession(UUID sessionId);
}
