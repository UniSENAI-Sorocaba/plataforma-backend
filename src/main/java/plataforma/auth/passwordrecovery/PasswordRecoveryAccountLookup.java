package plataforma.auth.passwordrecovery;

import java.util.Optional;
import java.util.UUID;

/** Port used by the account module to avoid issuing tokens for unknown subjects. */
@FunctionalInterface
public interface PasswordRecoveryAccountLookup {
    Optional<UUID> findUserIdByEmail(String normalizedEmail);
}
