package plataforma.auth.passwordrecovery;

/** Deliberately generic failure so token state is not disclosed to callers. */
public class InvalidPasswordRecoveryTokenException extends RuntimeException {
    public InvalidPasswordRecoveryTokenException() {
        super("password recovery token is invalid or expired");
    }
}
