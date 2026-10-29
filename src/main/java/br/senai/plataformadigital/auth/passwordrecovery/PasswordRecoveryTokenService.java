package br.senai.plataformadigital.auth.passwordrecovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates and manages opaque password-recovery tokens.
 *
 * <p>Generation returns the raw value exactly once for a delivery adapter.
 * Validation does not consume it; reset flows must call {@link #consume(String)}
 * so the repository can enforce single use atomically.</p>
 */
public final class PasswordRecoveryTokenService {
    private final PasswordRecoveryTokenRepository repository;
    private final Clock clock;
    private final PasswordRecoveryTokenPolicy policy;
    private final SecureRandom secureRandom;

    public PasswordRecoveryTokenService(
            PasswordRecoveryTokenRepository repository,
            Clock clock,
            PasswordRecoveryTokenPolicy policy) {
        this(repository, clock, policy, new SecureRandom());
    }

    PasswordRecoveryTokenService(
            PasswordRecoveryTokenRepository repository,
            Clock clock,
            PasswordRecoveryTokenPolicy policy,
            SecureRandom secureRandom) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    /** Issues a token for a normalized account subject, revoking older active tokens first. */
    public synchronized IssuedPasswordRecoveryToken issueForSubject(String subject) {
        String normalizedSubject = normalizeSubject(subject);
        Instant issuedAt = clock.instant();
        repository.revokeActiveBySubject(normalizedSubject, issuedAt);

        String rawToken = generateRawToken();
        Instant expiresAt = issuedAt.plus(policy.lifetime());
        PasswordRecoveryToken stored = PasswordRecoveryToken.issued(
                UUID.randomUUID(),
                normalizedSubject,
                hash(rawToken),
                issuedAt,
                expiresAt);
        repository.save(stored);
        return new IssuedPasswordRecoveryToken(rawToken, expiresAt);
    }

    /** Convenience name for account integrations whose subject is an e-mail address. */
    public IssuedPasswordRecoveryToken issueForEmail(String email) {
        return issueForSubject(email);
    }

    public PasswordRecoveryTokenValidation validate(String rawToken) {
        Instant now = clock.instant();
        Optional<PasswordRecoveryToken> token = find(rawToken);
        if (token.isEmpty()) {
            return new PasswordRecoveryTokenValidation(PasswordRecoveryTokenStatus.NOT_FOUND, null);
        }
        PasswordRecoveryToken stored = token.orElseThrow();
        return new PasswordRecoveryTokenValidation(statusOf(stored, now), stored.expiresAt());
    }

    public boolean isValid(String rawToken) {
        return validate(rawToken).isValid();
    }

    /** Atomically marks an active token as used and returns its subject to the reset flow. */
    public PasswordRecoveryTokenConsumption consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return PasswordRecoveryTokenConsumption.rejected(PasswordRecoveryTokenStatus.NOT_FOUND);
        }
        Instant now = clock.instant();
        Optional<PasswordRecoveryToken> consumed = repository.consumeIfActive(hash(rawToken), now);
        if (consumed.isPresent()) {
            PasswordRecoveryToken token = consumed.orElseThrow();
            return PasswordRecoveryTokenConsumption.consumed(token.subject(), now);
        }
        return PasswordRecoveryTokenConsumption.rejected(validate(rawToken).status());
    }

    /** Revokes one active token without revealing whether a token existed. */
    public boolean revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return repository.revokeIfActive(hash(rawToken), clock.instant());
    }

    /** Revokes all active tokens belonging to one normalized account subject. */
    public int revokeForSubject(String subject) {
        return repository.revokeActiveBySubject(normalizeSubject(subject), clock.instant());
    }

    static String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        return subject.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<PasswordRecoveryToken> find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHash(hash(rawToken));
    }

    private PasswordRecoveryTokenStatus statusOf(PasswordRecoveryToken token, Instant now) {
        if (token.revokedAt() != null) {
            return PasswordRecoveryTokenStatus.REVOKED;
        }
        if (token.usedAt() != null) {
            return PasswordRecoveryTokenStatus.USED;
        }
        if (!now.isBefore(token.expiresAt())) {
            return PasswordRecoveryTokenStatus.EXPIRED;
        }
        return PasswordRecoveryTokenStatus.VALID;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[policy.entropyBytes()];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
