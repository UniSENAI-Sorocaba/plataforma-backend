package plataforma.auth.passwordrecovery;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/** Completes a password reset after the token has been consumed atomically. */
public class PasswordResetService {

    private final PasswordRecoveryTokenService tokenService;
    private final PasswordHasher passwordHasher;
    private final PasswordRecoveryAccountStore accountStore;
    private final PasswordRecoverySessionRevoker sessionRevoker;

    public PasswordResetService(
            PasswordRecoveryTokenService tokenService,
            PasswordHasher passwordHasher,
            PasswordRecoveryAccountStore accountStore,
            PasswordRecoverySessionRevoker sessionRevoker) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "sessionRevoker must not be null");
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword must not be blank");
        }
        PasswordRecoveryTokenConsumption consumption = tokenService.consume(rawToken);
        if (!consumption.isConsumed()) {
            throw new InvalidPasswordRecoveryTokenException();
        }

        var userId = consumption.userId();
        accountStore.updatePasswordHash(userId, passwordHasher.hash(newPassword));
        sessionRevoker.revokeAllSessions(userId);
        tokenService.revokeForUser(userId);
    }
}
