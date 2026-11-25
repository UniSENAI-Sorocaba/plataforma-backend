package plataforma.auth.passwordrecovery;

/** Port supplied by BE-005; implementations must use the approved password hash. */
@FunctionalInterface
public interface PasswordHasher {
    String hash(String rawPassword);
}
