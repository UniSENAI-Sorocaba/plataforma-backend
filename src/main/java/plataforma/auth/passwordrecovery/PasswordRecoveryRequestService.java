package plataforma.auth.passwordrecovery;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Coordinates neutral recovery requests without exposing account existence. */
public final class PasswordRecoveryRequestService {
    static final int MAX_ISSUANCES_PER_WINDOW = 3;
    static final Duration ISSUANCE_WINDOW = Duration.ofHours(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryRequestService.class);

    private final PasswordRecoveryAccountLookup accountLookup;
    private final PasswordRecoveryTokenService tokenService;
    private final PasswordRecoveryTokenDelivery tokenDelivery;
    private final Executor deliveryExecutor;

    public PasswordRecoveryRequestService(
            PasswordRecoveryAccountLookup accountLookup,
            PasswordRecoveryTokenService tokenService,
            PasswordRecoveryTokenDelivery tokenDelivery) {
        this(accountLookup, tokenService, tokenDelivery, Runnable::run);
    }

    public PasswordRecoveryRequestService(
            PasswordRecoveryAccountLookup accountLookup,
            PasswordRecoveryTokenService tokenService,
            PasswordRecoveryTokenDelivery tokenDelivery,
            Executor deliveryExecutor) {
        this.accountLookup = Objects.requireNonNull(accountLookup, "accountLookup must not be null");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.tokenDelivery = Objects.requireNonNull(tokenDelivery, "tokenDelivery must not be null");
        this.deliveryExecutor = Objects.requireNonNull(deliveryExecutor, "deliveryExecutor must not be null");
    }

    /**
     * Requests recovery for an e-mail. The method has no result that can
     * distinguish a known account from an unknown one.
     */
    public void request(String email) {
        String normalizedEmail = PasswordRecoveryTokenService.normalizeEmail(email);
        Optional<UUID> userId = accountLookup.findUserIdByEmail(normalizedEmail);
        deliveryExecutor.execute(() -> userId.ifPresent(
                id -> issueAndDeliverSafely(normalizedEmail, id)));
    }

    private void issueAndDeliverSafely(String normalizedEmail, UUID userId) {
        try {
            Optional<IssuedPasswordRecoveryToken> issued = tokenService.issueForUserWithinRateLimit(
                    userId, MAX_ISSUANCES_PER_WINDOW, ISSUANCE_WINDOW);
            if (issued.isEmpty()) {
                return;
            }
            IssuedPasswordRecoveryToken token = issued.orElseThrow();
            tokenDelivery.deliver(normalizedEmail, token.rawToken(), token.expiresAt());
        } catch (RuntimeException exception) {
            LOGGER.warn("falha ao enviar recuperação de senha");
        }
    }
}
