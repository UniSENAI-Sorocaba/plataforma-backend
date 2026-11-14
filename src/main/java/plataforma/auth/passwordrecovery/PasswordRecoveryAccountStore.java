package plataforma.auth.passwordrecovery;

import java.util.UUID;

/** Port supplied by the account/credential module to persist a new password hash. */
@FunctionalInterface
public interface PasswordRecoveryAccountStore {
    void updatePasswordHash(UUID userId, String passwordHash);
}
