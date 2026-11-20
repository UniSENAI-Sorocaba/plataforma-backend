package br.senai.plataformadigital.auth;

import java.time.Instant;
import java.util.UUID;

public record IssuedRefreshToken(String rawToken, UUID sessionId, UUID familyId, Instant expiresAt) {
}
