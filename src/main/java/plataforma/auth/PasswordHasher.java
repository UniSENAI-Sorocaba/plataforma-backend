package plataforma.auth;

import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Isolated Argon2id password hashing; callers must never expose stored hashes to clients. */
public final class PasswordHasher {

    private static final Pattern ARGON2ID_HASH = Pattern.compile(
            "\\A\\$argon2id\\$v=19\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$([A-Za-z0-9+/]+)\\$([A-Za-z0-9+/]+)\\z");
    private static final int MAX_ENCODED_FIELD_BYTES = 128;

    private final Argon2PasswordEncoder encoder;
    private final Argon2Properties properties;

    PasswordHasher(Argon2PasswordEncoder encoder, Argon2Properties properties) {
        this.encoder = Objects.requireNonNull(encoder, "encoder must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /** Hashes a password using a fresh cryptographically secure salt. */
    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        return encoder.encode(password);
    }

    /** Returns false for malformed hashes without exposing encoder details. */
    public boolean verify(String password, String encodedHash) {
        if (password == null || !isSupportedHash(encodedHash)) {
            return false;
        }
        try {
            return encoder.matches(password, encodedHash);
        }
        catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Reports whether a verified hash should be replaced with current configured parameters. */
    public boolean needsRehash(String encodedHash) {
        HashParameters parameters = parseSupportedHash(encodedHash);
        if (parameters == null) {
            return true;
        }
        return parameters.memoryKib() < properties.getMemoryKib()
                || parameters.iterations() < properties.getIterations()
                || parameters.parallelism() < properties.getParallelism()
                || parameters.saltLength() < properties.getSaltLength()
                || parameters.hashLength() < properties.getHashLength();
    }

    private boolean isSupportedHash(String encodedHash) {
        return parseSupportedHash(encodedHash) != null;
    }

    // TODO(BE-005): lowering configured costs below already-issued hashes needs a migration plan.
    private HashParameters parseSupportedHash(String encodedHash) {
        if (encodedHash == null || encodedHash.length() > 512) {
            return null;
        }
        Matcher matcher = ARGON2ID_HASH.matcher(encodedHash);
        if (!matcher.matches()) {
            return null;
        }
        try {
            int memoryKib = Integer.parseInt(matcher.group(1));
            int iterations = Integer.parseInt(matcher.group(2));
            int parallelism = Integer.parseInt(matcher.group(3));
            if (parallelism < 1 || parallelism > properties.getParallelism()) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(matcher.group(4));
            byte[] hash = Base64.getDecoder().decode(matcher.group(5));
            if (memoryKib < 8L * parallelism
                    || memoryKib > properties.getMemoryKib()
                    || iterations < 1
                    || iterations > properties.getIterations()
                    || salt.length < 16
                    || salt.length > MAX_ENCODED_FIELD_BYTES
                    || hash.length < 16
                    || hash.length > MAX_ENCODED_FIELD_BYTES) {
                return null;
            }
            return new HashParameters(memoryKib, iterations, parallelism, salt.length, hash.length);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record HashParameters(int memoryKib, int iterations, int parallelism, int saltLength, int hashLength) {
    }
}
