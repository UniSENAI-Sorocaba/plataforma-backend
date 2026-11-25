package plataforma.auth.passwordrecovery;

/** Internal lifecycle state of a password-recovery token. */
public enum PasswordRecoveryTokenStatus {
    VALID,
    NOT_FOUND,
    EXPIRED,
    USED,
    REVOKED,
    CONSUMED
}
