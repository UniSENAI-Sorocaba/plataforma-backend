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
    private static final int MAX_ENCODED_HASH_LENGTH = 512;

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
        return parameters.memoryKib() != properties.getMemoryKib()
                || parameters.iterations() != properties.getIterations()
                || parameters.parallelism() != properties.getParallelism()
                || parameters.saltLength() != properties.getSaltLength()
                || parameters.hashLength() != properties.getHashLength();
    }

    private boolean isSupportedHash(String encodedHash) {
        return parseSupportedHash(encodedHash) != null;
    }

    private HashParameters parseSupportedHash(String encodedHash) {
        if (encodedHash == null || encodedHash.length() > MAX_ENCODED_HASH_LENGTH) {
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
            Argon2Properties.HashSanityLimits limits = properties.getHashSanity();
            if (memoryKib < limits.getMinMemoryKib()
                    || memoryKib > limits.getMaxMemoryKib()
                    || iterations < limits.getMinIterations()
                    || iterations > limits.getMaxIterations()
                    || parallelism < limits.getMinParallelism()
                    || parallelism > limits.getMaxParallelism()
                    || memoryKib < 8L * parallelism) {
                return null;
            }
            if (matcher.group(4).length() > maxBase64Length(limits.getMaxSaltLength())
                    || matcher.group(5).length() > maxBase64Length(limits.getMaxHashLength())) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(matcher.group(4));
            byte[] hash = Base64.getDecoder().decode(matcher.group(5));
            if (salt.length < limits.getMinSaltLength()
                    || salt.length > limits.getMaxSaltLength()
                    || hash.length < limits.getMinHashLength()
                    || hash.length > limits.getMaxHashLength()) {
                return null;
            }
            return new HashParameters(memoryKib, iterations, parallelism, salt.length, hash.length);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int maxBase64Length(int byteLength) {
        return ((byteLength + 2) / 3) * 4;
    }

    private record HashParameters(int memoryKib, int iterations, int parallelism, int saltLength, int hashLength) {
    }
}
