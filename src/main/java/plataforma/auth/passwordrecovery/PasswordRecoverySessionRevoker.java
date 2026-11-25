package plataforma.auth.passwordrecovery;

import java.util.UUID;

/** Port supplied by the session module to invalidate all sessions after a reset. */
@FunctionalInterface
public interface PasswordRecoverySessionRevoker {
    void revokeAllSessions(UUID userId);
}
