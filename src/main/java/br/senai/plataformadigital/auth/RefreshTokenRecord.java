package br.senai.plataformadigital.auth;

import java.time.Instant;
import java.util.UUID;

record RefreshTokenRecord(
        UUID id,
        UUID userId,
        UUID sessionId,
        UUID familyId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt) {

    RefreshTokenRecord usedAt(Instant timestamp) {
        return new RefreshTokenRecord(id, userId, sessionId, familyId, tokenHash, issuedAt, expiresAt, timestamp, revokedAt);
    }

    RefreshTokenRecord revokedAt(Instant timestamp) {
        return new RefreshTokenRecord(id, userId, sessionId, familyId, tokenHash, issuedAt, expiresAt, usedAt, timestamp);
    }
}
