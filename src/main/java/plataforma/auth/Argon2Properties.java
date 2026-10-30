package plataforma.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Argon2id parameters. Defaults follow OWASP minimum guidance and require target calibration. */
@Validated
@ConfigurationProperties(prefix = "platform.security.password.argon2")
public class Argon2Properties {

    @Min(19_456)
    private int memoryKib = 19_456;

    @Min(2)
    private int iterations = 2;

    @Min(1)
    private int parallelism = 1;

    @Min(16)
    @Max(128)
    private int saltLength = 16;

    @Min(32)
    @Max(128)
    private int hashLength = 32;

    @Valid
    private final HashSanityLimits hashSanity = new HashSanityLimits();

    public int getMemoryKib() {
        return memoryKib;
    }

    public void setMemoryKib(int memoryKib) {
        this.memoryKib = memoryKib;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getSaltLength() {
        return saltLength;
    }

    public void setSaltLength(int saltLength) {
        this.saltLength = saltLength;
    }

    public int getHashLength() {
        return hashLength;
    }

    public void setHashLength(int hashLength) {
        this.hashLength = hashLength;
    }

    public HashSanityLimits getHashSanity() {
        return hashSanity;
    }

    @AssertTrue(message = "Configured Argon2 parameters must be within hash sanity limits")
    public boolean isConfiguredWithinHashSanityLimits() {
        return hashSanity.isValid()
                && memoryKib >= hashSanity.minMemoryKib && memoryKib <= hashSanity.maxMemoryKib
                && iterations >= hashSanity.minIterations && iterations <= hashSanity.maxIterations
                && parallelism >= hashSanity.minParallelism && parallelism <= hashSanity.maxParallelism
                && saltLength >= hashSanity.minSaltLength && saltLength <= hashSanity.maxSaltLength
                && hashLength >= hashSanity.minHashLength && hashLength <= hashSanity.maxHashLength;
    }

    /** Independent absolute bounds for parsing hashes already stored in PHC format. */
    public static class HashSanityLimits {

        @Min(8)
        @Max(262_144)
        private int minMemoryKib = 8;

        @Min(8)
        @Max(262_144)
        private int maxMemoryKib = 262_144;

        @Min(1)
        @Max(10)
        private int minIterations = 1;

        @Min(1)
        @Max(10)
        private int maxIterations = 10;

        @Min(1)
        @Max(16)
        private int minParallelism = 1;

        @Min(1)
        @Max(16)
        private int maxParallelism = 16;

        @Min(16)
        @Max(128)
        private int minSaltLength = 16;

        @Min(16)
        @Max(128)
        private int maxSaltLength = 128;

        @Min(16)
        @Max(128)
        private int minHashLength = 16;

        @Min(16)
        @Max(128)
        private int maxHashLength = 128;

        @AssertTrue(message = "Argon2 hash sanity minima must not exceed maxima")
        public boolean isValid() {
            return minMemoryKib <= maxMemoryKib
                    && minIterations <= maxIterations
                    && minParallelism <= maxParallelism
                    && minSaltLength <= maxSaltLength
                    && minHashLength <= maxHashLength;
        }

        public int getMinMemoryKib() {
            return minMemoryKib;
        }

        public void setMinMemoryKib(int value) {
            minMemoryKib = value;
        }

        public int getMaxMemoryKib() {
            return maxMemoryKib;
        }

        public void setMaxMemoryKib(int value) {
            maxMemoryKib = value;
        }

        public int getMinIterations() {
            return minIterations;
        }

        public void setMinIterations(int value) {
            minIterations = value;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int value) {
            maxIterations = value;
        }

        public int getMinParallelism() {
            return minParallelism;
        }

        public void setMinParallelism(int value) {
            minParallelism = value;
        }

        public int getMaxParallelism() {
            return maxParallelism;
        }

        public void setMaxParallelism(int value) {
            maxParallelism = value;
        }

        public int getMinSaltLength() {
            return minSaltLength;
        }

        public void setMinSaltLength(int value) {
            minSaltLength = value;
        }

        public int getMaxSaltLength() {
            return maxSaltLength;
        }

        public void setMaxSaltLength(int value) {
            maxSaltLength = value;
        }

        public int getMinHashLength() {
            return minHashLength;
        }

        public void setMinHashLength(int value) {
            minHashLength = value;
        }

        public int getMaxHashLength() {
            return maxHashLength;
        }

        public void setMaxHashLength(int value) {
            maxHashLength = value;
        }
    }
}
