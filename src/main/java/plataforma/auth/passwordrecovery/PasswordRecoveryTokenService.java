package plataforma.auth.passwordrecovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
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

    /** Issues a token for a user, revoking older active tokens first. */
    public synchronized IssuedPasswordRecoveryToken issueForUser(UUID userId) {
        return issueForUser(userId, clock.instant());
    }

    /**
     * Issues at most {@code maximum} tokens for a user during the supplied window.
     * The check and issuance are serialized so concurrent requests cannot bypass it.
     */
    public synchronized Optional<IssuedPasswordRecoveryToken> issueForUserWithinRateLimit(
            UUID userId,
            int maximum,
            Duration window) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (maximum < 1) {
            throw new IllegalArgumentException("maximum must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        Instant issuedAt = clock.instant();
        if (repository.countIssuedSince(userId, issuedAt.minus(window)) >= maximum) {
            return Optional.empty();
        }
        return Optional.of(issueForUser(userId, issuedAt));
    }

    private IssuedPasswordRecoveryToken issueForUser(UUID userId, Instant issuedAt) {
        Objects.requireNonNull(userId, "userId must not be null");
        repository.revokeActiveByUserId(userId, issuedAt);

        String rawToken = generateRawToken();
        Instant expiresAt = issuedAt.plus(policy.lifetime());
        PasswordRecoveryToken stored = PasswordRecoveryToken.issued(
                UUID.randomUUID(),
                userId,
                hash(rawToken),
                issuedAt,
                expiresAt);
        repository.save(stored);
        return new IssuedPasswordRecoveryToken(rawToken, expiresAt);
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

    /** Atomically marks an active token as used and returns its user id to the reset flow. */
    public PasswordRecoveryTokenConsumption consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return PasswordRecoveryTokenConsumption.rejected(PasswordRecoveryTokenStatus.NOT_FOUND);
        }
        Instant now = clock.instant();
        Optional<PasswordRecoveryToken> consumed = repository.consumeIfActive(hash(rawToken), now);
        if (consumed.isPresent()) {
            PasswordRecoveryToken token = consumed.orElseThrow();
            return PasswordRecoveryTokenConsumption.consumed(token.userId(), now);
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

    /** Revokes all active tokens belonging to one user. */
    public int revokeForUser(UUID userId) {
        return repository.revokeActiveByUserId(Objects.requireNonNull(userId, "userId must not be null"), clock.instant());
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
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
