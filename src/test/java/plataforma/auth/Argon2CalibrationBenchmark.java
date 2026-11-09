package plataforma.auth;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * Manual calibration harness. Surefire's default class patterns do not include this class;
 * invoke explicitly with {@code mvn -Dtest=Argon2CalibrationBenchmark test}.
 */
class Argon2CalibrationBenchmark {

    private static final String BENCHMARK_ONLY_INPUT = "fictional-benchmark-input-2026";
    private static final int WARMUP_RUNS = 1;
    private static final int MEASURED_RUNS = 5;
    private static final Candidate[] CANDIDATES = {
        new Candidate(19_456, 2, 1),
        new Candidate(19_456, 3, 1),
        new Candidate(32_768, 2, 1),
        new Candidate(47_104, 1, 1),
        new Candidate(32_768, 2, 2),
        new Candidate(65_536, 2, 1),
        new Candidate(65_536, 3, 1),
        new Candidate(98_304, 2, 1),
        new Candidate(131_072, 2, 1)
    };

    @Test
    void printsAverageHashLatencyForCandidateParameters() {
        System.out.println("Argon2id calibration: one warmup plus five measured hashes per candidate");
        for (Candidate candidate : CANDIDATES) {
            Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                    16, 32, candidate.parallelism(), candidate.memoryKib(), candidate.iterations());
            for (int i = 0; i < WARMUP_RUNS; i++) {
                encoder.encode(BENCHMARK_ONLY_INPUT);
            }

            long totalNanos = 0;
            long minNanos = Long.MAX_VALUE;
            long maxNanos = Long.MIN_VALUE;
            for (int i = 0; i < MEASURED_RUNS; i++) {
                long started = System.nanoTime();
                encoder.encode(BENCHMARK_ONLY_INPUT);
                long elapsed = System.nanoTime() - started;
                totalNanos += elapsed;
                minNanos = Math.min(minNanos, elapsed);
                maxNanos = Math.max(maxNanos, elapsed);
            }

            System.out.printf(Locale.ROOT,
                    "m=%d KiB, t=%d, p=%d -> mean=%.1f ms, min=%.1f ms, max=%.1f ms%n",
                    candidate.memoryKib(), candidate.iterations(), candidate.parallelism(),
                    millis(totalNanos / (double) MEASURED_RUNS), millis(minNanos), millis(maxNanos));
        }
    }

    private static double millis(double nanos) {
        return nanos / 1_000_000.0;
    }

    private record Candidate(int memoryKib, int iterations, int parallelism) {
    }
}
