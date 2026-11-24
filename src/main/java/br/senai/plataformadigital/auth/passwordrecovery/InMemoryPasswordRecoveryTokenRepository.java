package br.senai.plataformadigital.auth.passwordrecovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local adapter used by tests and local development until AUTH-10-DB
 * supplies the durable implementation.
 */
public final class InMemoryPasswordRecoveryTokenRepository implements PasswordRecoveryTokenRepository {
    private final Map<UUID, PasswordRecoveryToken> tokens = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        return tokens.values().stream()
                .filter(token -> sameHash(token.tokenHash(), tokenHash))
                .findFirst();
    }

    @Override
    public synchronized void save(PasswordRecoveryToken token) {
        tokens.put(Objects.requireNonNull(token, "token must not be null").id(), token);
    }

    @Override
    public synchronized int revokeActiveBySubject(String subject, Instant revokedAt) {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        int revoked = 0;
        for (PasswordRecoveryToken token : tokens.values()) {
            if (token.subject().equals(subject) && token.isActiveAt(revokedAt)) {
                tokens.put(token.id(), token.markRevokedAt(revokedAt));
                revoked++;
            }
        }
        return revoked;
    }

    @Override
    public synchronized Optional<PasswordRecoveryToken> consumeIfActive(String tokenHash, Instant consumedAt) {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(consumedAt, "consumedAt must not be null");
        Optional<PasswordRecoveryToken> token = findByTokenHash(tokenHash)
                .filter(candidate -> candidate.isActiveAt(consumedAt));
        if (token.isEmpty()) {
            return Optional.empty();
        }
        PasswordRecoveryToken consumed = token.orElseThrow().markUsedAt(consumedAt);
        tokens.put(consumed.id(), consumed);
        return Optional.of(consumed);
    }

    @Override
    public synchronized boolean revokeIfActive(String tokenHash, Instant revokedAt) {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        Optional<PasswordRecoveryToken> token = findByTokenHash(tokenHash)
                .filter(candidate -> candidate.isActiveAt(revokedAt));
        if (token.isEmpty()) {
            return false;
        }
        PasswordRecoveryToken revoked = token.orElseThrow().markRevokedAt(revokedAt);
        tokens.put(revoked.id(), revoked);
        return true;
    }

    private boolean sameHash(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }
}
