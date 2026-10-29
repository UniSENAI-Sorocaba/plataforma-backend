package br.senai.plataformadigital.auth.passwordrecovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PasswordRecoveryTokenServiceTests {
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    @Test
    void generatesOpaqueTokenAndStoresOnlyItsDigest() throws Exception {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));

        IssuedPasswordRecoveryToken issued = service.issueForEmail(" User@Example.COM ");

        assertThat(issued.rawToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(issued.rawToken()).doesNotContain("User", "Example");
        assertThat(repository.findByTokenHash(issued.rawToken())).isEmpty();
        assertThat(repository.findByTokenHash(hash(issued.rawToken()))).isPresent()
                .get()
                .satisfies(token -> {
                    assertThat(token.subject()).isEqualTo("user@example.com");
                    assertThat(token.tokenHash()).isEqualTo(hash(issued.rawToken()));
                });
        assertThat(issued.toString()).doesNotContain(issued.rawToken());
    }

    @Test
    void validatesUntilTheExactExpirationInstant() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        MutableClock clock = new MutableClock(NOW);
        PasswordRecoveryTokenService service = new PasswordRecoveryTokenService(
                repository,
                clock,
                PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(20)));
        String rawToken = service.issueForEmail("user@example.com").rawToken();

        assertThat(service.validate(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.VALID);
        clock.advance(Duration.ofMinutes(20));
        assertThat(service.validate(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.EXPIRED);
        assertThat(service.consume(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.EXPIRED);
    }

    @Test
    void newIssuanceRevokesPreviousActiveTokenForTheSameSubject() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));

        String first = service.issueForEmail("user@example.com").rawToken();
        String second = service.issueForEmail("USER@example.com").rawToken();

        assertThat(service.validate(first).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
        assertThat(service.validate(second).status()).isEqualTo(PasswordRecoveryTokenStatus.VALID);
    }

    @Test
    void explicitRevocationPreventsConsumption() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String rawToken = service.issueForEmail("user@example.com").rawToken();

        assertThat(service.revoke(rawToken)).isTrue();
        assertThat(service.revoke(rawToken)).isFalse();
        assertThat(service.consume(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
    }

    @Test
    void consumptionReturnsNormalizedSubjectAndRevocationCanTargetTheSubject() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String consumed = service.issueForEmail("User@Example.com").rawToken();
        service.consume(consumed);
        String active = service.issueForEmail("other@example.com").rawToken();

        PasswordRecoveryTokenConsumption result = service.consume(active);

        assertThat(result.status()).isEqualTo(PasswordRecoveryTokenStatus.CONSUMED);
        assertThat(result.subject()).isEqualTo("other@example.com");
        assertThat(result.consumedAt()).isEqualTo(NOW);
        assertThat(service.isValid(active)).isFalse();

        String revocable = service.issueForEmail("other@example.com").rawToken();
        assertThat(service.revokeForSubject(" OTHER@example.com ")).isEqualTo(1);
        assertThat(service.validate(revocable).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
    }

    @Test
    void consumesTokenOnlyOnceEvenWhenRequestsRace() throws Exception {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String rawToken = service.issueForEmail("user@example.com").rawToken();
        int attempts = 12;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            List<Future<PasswordRecoveryTokenConsumption>> results = IntStream.range(0, attempts)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await();
                        return service.consume(rawToken);
                    }))
                    .toList();
            start.countDown();

            List<PasswordRecoveryTokenConsumption> consumptions = results.stream().map(this::get).toList();
            assertThat(consumptions.stream().filter(PasswordRecoveryTokenConsumption::isConsumed)).hasSize(1);
            assertThat(consumptions.stream().filter(result -> !result.isConsumed()))
                    .allSatisfy(result -> assertThat(result.status()).isEqualTo(PasswordRecoveryTokenStatus.USED));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requestServiceDoesNotRevealWhetherTheSubjectExists() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService tokenService = service(repository, Duration.ofMinutes(20));
        AtomicReference<String> delivered = new AtomicReference<>();
        PasswordRecoveryRequestService requestService = new PasswordRecoveryRequestService(
                subject -> subject.equals("known@example.com"),
                tokenService,
                (subject, token, expiresAt) -> delivered.set(subject + ":" + token));

        requestService.request("unknown@example.com");
        assertThat(delivered.get()).isNull();
        assertThat(repository.findByTokenHash("unknown@example.com")).isEmpty();

        requestService.request(" KNOWN@example.com ");
        assertThat(delivered).isNotNull();
        assertThat(delivered.get()).startsWith("known@example.com:");
    }

    @Test
    void rejectsInvalidPolicyAndSubject() {
        assertThatThrownBy(() -> PasswordRecoveryTokenPolicy.withLifetime(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(1)))
                .doesNotThrowAnyException();

        PasswordRecoveryTokenService service = service(
                new InMemoryPasswordRecoveryTokenRepository(), Duration.ofMinutes(20));
        assertThatThrownBy(() -> service.issueForSubject("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.validate(null).status()).isEqualTo(PasswordRecoveryTokenStatus.NOT_FOUND);
        assertThat(service.revoke(" ")).isFalse();
    }

    private PasswordRecoveryTokenService service(
            InMemoryPasswordRecoveryTokenRepository repository,
            Duration lifetime) {
        return new PasswordRecoveryTokenService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                PasswordRecoveryTokenPolicy.withLifetime(lifetime));
    }

    private PasswordRecoveryTokenConsumption get(Future<PasswordRecoveryTokenConsumption> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new AssertionError("concurrent token consumption failed", exception);
        }
    }

    private static String hash(String rawToken) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
