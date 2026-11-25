package plataforma.auth.passwordrecovery;

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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PasswordRecoveryTokenServiceTests {
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void generatesOpaqueTokenAndStoresOnlyItsDigestAndUserId() throws Exception {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));

        IssuedPasswordRecoveryToken issued = service.issueForUser(USER_ID);

        assertThat(issued.rawToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(repository.findByTokenHash(issued.rawToken())).isEmpty();
        assertThat(repository.findByTokenHash(hash(issued.rawToken()))).isPresent()
                .get()
                .satisfies(token -> {
                    assertThat(token.userId()).isEqualTo(USER_ID);
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
        String rawToken = service.issueForUser(USER_ID).rawToken();

        assertThat(service.validate(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.VALID);
        clock.advance(Duration.ofMinutes(20));
        assertThat(service.validate(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.EXPIRED);
        assertThat(service.consume(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.EXPIRED);
    }

    @Test
    void newIssuanceRevokesPreviousActiveTokenForTheSameUser() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));

        String first = service.issueForUser(USER_ID).rawToken();
        String second = service.issueForUser(USER_ID).rawToken();

        assertThat(service.validate(first).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
        assertThat(service.validate(second).status()).isEqualTo(PasswordRecoveryTokenStatus.VALID);
    }

    @Test
    void explicitRevocationPreventsConsumption() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String rawToken = service.issueForUser(USER_ID).rawToken();

        assertThat(service.revoke(rawToken)).isTrue();
        assertThat(service.revoke(rawToken)).isFalse();
        assertThat(service.consume(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
    }

    @Test
    void consumptionReturnsUserIdAndRevocationCanTargetTheUser() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String rawToken = service.issueForUser(USER_ID).rawToken();

        PasswordRecoveryTokenConsumption result = service.consume(rawToken);

        assertThat(result.status()).isEqualTo(PasswordRecoveryTokenStatus.CONSUMED);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.consumedAt()).isEqualTo(NOW);
        assertThat(service.isValid(rawToken)).isFalse();

        UUID otherUser = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        String revocable = service.issueForUser(otherUser).rawToken();
        assertThat(service.revokeForUser(otherUser)).isEqualTo(1);
        assertThat(service.validate(revocable).status()).isEqualTo(PasswordRecoveryTokenStatus.REVOKED);
    }

    @Test
    void consumesTokenOnlyOnceEvenWhenRequestsRace() throws Exception {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService service = service(repository, Duration.ofMinutes(20));
        String rawToken = service.issueForUser(USER_ID).rawToken();
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
    void requestServiceDoesNotRevealWhetherTheAccountExists() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService tokenService = service(repository, Duration.ofMinutes(20));
        AtomicReference<String> delivered = new AtomicReference<>();
        PasswordRecoveryRequestService requestService = new PasswordRecoveryRequestService(
                email -> email.equals("known@example.com") ? Optional.of(USER_ID) : Optional.empty(),
                tokenService,
                (email, token, expiresAt) -> delivered.set(email + ":" + token));

        requestService.request("unknown@example.com");
        assertThat(delivered.get()).isNull();

        requestService.request(" KNOWN@example.com ");
        assertThat(delivered).isNotNull();
        assertThat(delivered.get()).startsWith("known@example.com:");
    }

    @Test
    void requestServiceStopsAfterThreeIssuancesPerHour() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService tokenService = service(repository, Duration.ofMinutes(20));
        AtomicInteger deliveries = new AtomicInteger();
        PasswordRecoveryRequestService requestService = new PasswordRecoveryRequestService(
                email -> Optional.of(USER_ID), tokenService,
                (email, token, expiresAt) -> deliveries.incrementAndGet());

        IntStream.range(0, 4).forEach(index -> requestService.request("known@example.com"));

        assertThat(deliveries).hasValue(3);
        assertThat(repository.countIssuedSince(USER_ID, NOW.minus(Duration.ofHours(1)))).isEqualTo(3);
    }

    @Test
    void requestServiceEnqueuesIssuanceAndDelivery() {
        PasswordRecoveryTokenService tokenService = service(
                new InMemoryPasswordRecoveryTokenRepository(), Duration.ofMinutes(20));
        AtomicInteger deliveries = new AtomicInteger();
        AtomicReference<Runnable> queued = new AtomicReference<>();
        PasswordRecoveryRequestService requestService = new PasswordRecoveryRequestService(
                email -> Optional.of(USER_ID), tokenService,
                (email, token, expiresAt) -> deliveries.incrementAndGet(),
                queued::set);

        requestService.request("known@example.com");

        assertThat(deliveries).hasValue(0);
        queued.get().run();
        assertThat(deliveries).hasValue(1);
    }

    @Test
    void deliveryFailureDoesNotEscapeTheRequest() {
        PasswordRecoveryTokenService tokenService = service(
                new InMemoryPasswordRecoveryTokenRepository(), Duration.ofMinutes(20));
        PasswordRecoveryRequestService requestService = new PasswordRecoveryRequestService(
                email -> Optional.of(USER_ID), tokenService,
                (email, token, expiresAt) -> { throw new IllegalStateException("mail unavailable"); });

        assertThatCode(() -> requestService.request("known@example.com")).doesNotThrowAnyException();
    }

    @Test
    void resetHashesTheNewPasswordAndRevokesSessionsAndOtherTokens() {
        InMemoryPasswordRecoveryTokenRepository repository = new InMemoryPasswordRecoveryTokenRepository();
        PasswordRecoveryTokenService tokenService = service(repository, Duration.ofMinutes(20));
        String rawToken = tokenService.issueForUser(USER_ID).rawToken();
        AtomicReference<UUID> storedUser = new AtomicReference<>();
        AtomicReference<String> storedHash = new AtomicReference<>();
        AtomicReference<UUID> revokedUser = new AtomicReference<>();
        PasswordResetService resetService = new PasswordResetService(
                tokenService,
                password -> "argon2(" + password + ")",
                (userId, passwordHash) -> {
                    storedUser.set(userId);
                    storedHash.set(passwordHash);
                },
                revokedUser::set);

        resetService.reset(rawToken, "NovaSenha#123");

        assertThat(storedUser).hasValue(USER_ID);
        assertThat(storedHash).hasValue("argon2(NovaSenha#123)");
        assertThat(revokedUser).hasValue(USER_ID);
        assertThat(tokenService.validate(rawToken).status()).isEqualTo(PasswordRecoveryTokenStatus.USED);
    }

    @Test
    void rejectsInvalidPolicySubjectAndRateLimit() {
        assertThatThrownBy(() -> PasswordRecoveryTokenPolicy.withLifetime(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(1)))
                .doesNotThrowAnyException();

        PasswordRecoveryTokenService service = service(
                new InMemoryPasswordRecoveryTokenRepository(), Duration.ofMinutes(20));
        assertThatThrownBy(() -> service.issueForUser(null)).isInstanceOf(NullPointerException.class);
        assertThat(service.validate(null).status()).isEqualTo(PasswordRecoveryTokenStatus.NOT_FOUND);
        assertThat(service.revoke(" ")).isFalse();
        assertThatThrownBy(() -> service.issueForUserWithinRateLimit(
                USER_ID, 0, Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
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
