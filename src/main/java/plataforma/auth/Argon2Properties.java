package plataforma.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
}
