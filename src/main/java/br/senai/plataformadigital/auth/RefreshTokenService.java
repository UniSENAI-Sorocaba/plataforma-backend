package br.senai.plataformadigital.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Rotates opaque refresh tokens and revokes their family on reuse. */
public final class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration lifetime;
    private final SecureRandom secureRandom;

    public RefreshTokenService(RefreshTokenRepository repository, Clock clock, Duration lifetime) {
        this(repository, clock, lifetime, new SecureRandom());
    }

    RefreshTokenService(RefreshTokenRepository repository, Clock clock, Duration lifetime, SecureRandom secureRandom) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
    }

    public synchronized IssuedRefreshToken issue(UUID userId) {
        UUID sessionId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(lifetime);
        repository.createSession(sessionId, userId, familyId, issuedAt, expiresAt);
        return issue(userId, sessionId, familyId);
    }

    public synchronized IssuedRefreshToken rotate(String rawToken) {
        RefreshTokenRecord current = find(rawToken);
        Instant now = clock.instant();
        if (current.usedAt() != null || current.revokedAt() != null) {
            revokeFamily(current.familyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!now.isBefore(current.expiresAt())) {
            throw new InvalidRefreshTokenException();
        }
        repository.replace(current.usedAt(now));
        return issue(current.userId(), current.sessionId(), current.familyId());
    }

    public synchronized void revokeSession(UUID sessionId) {
        repository.revokeSession(sessionId, clock.instant());
    }

    public synchronized void revokeFamily(UUID familyId) {
        revokeFamily(familyId, clock.instant());
    }

    private void revokeFamily(UUID familyId, Instant now) {
        repository.revokeFamily(familyId, now);
    }

    private IssuedRefreshToken issue(UUID userId, UUID sessionId, UUID familyId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(lifetime);
        String rawToken = generateRawToken();
        repository.save(new RefreshTokenRecord(UUID.randomUUID(), userId, sessionId, familyId,
                hash(rawToken), issuedAt, expiresAt, null, null));
        return new IssuedRefreshToken(rawToken, userId, sessionId, familyId, expiresAt);
    }

    /** Revokes the session identified by a refresh token without revealing whether it existed. */
    public synchronized void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByHash(hash(rawToken)).ifPresent(record -> revokeSession(record.sessionId()));
    }

    private RefreshTokenRecord find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return repository.findByHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
