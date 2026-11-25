package plataforma.auth.passwordrecovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import plataforma.controller.PasswordRecoveryController;
import plataforma.exception.ApiExceptionHandler;

class PasswordRecoveryControllerTest {
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private InMemoryPasswordRecoveryTokenRepository repository;
    private PasswordRecoveryTokenService tokens;
    private AtomicReference<String> delivered;
    private AtomicReference<String> storedHash;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPasswordRecoveryTokenRepository();
        tokens = new PasswordRecoveryTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC),
                PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(15)));
        delivered = new AtomicReference<>();
        var requestService = new PasswordRecoveryRequestService(
                email -> email.equals("known@example.test") ? Optional.of(USER_ID) : Optional.empty(), tokens,
                (email, token, expiry) -> delivered.set(email + ":" + token));
        var hasher = (PasswordHasher) new Argon2PasswordEncoder(16, 32, 1, 19_456, 2)::encode;
        storedHash = new AtomicReference<>();
        var resetService = new PasswordResetService(tokens, hasher,
                (userId, hash) -> storedHash.set(hash), userId -> { });
        mvc = MockMvcBuilders.standaloneSetup(new PasswordRecoveryController(requestService, resetService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void recoveryResponseDoesNotRevealWhetherEmailExists() throws Exception {
        var known = mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"known@example.test\"}")).andReturn().getResponse();
        var unknown = mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.test\"}")).andReturn().getResponse();

        assertThat(known.getStatus()).isEqualTo(202);
        assertThat(unknown.getStatus()).isEqualTo(202);
        assertThat(known.getContentAsString()).isEqualTo(unknown.getContentAsString());
        assertThat(delivered.get()).startsWith("known@example.test:");
    }

    @Test
    void invalidEmailIsRejectedByHttpValidation() throws Exception {
        var response = mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}")).andReturn().getResponse();
        assertThat(response.getStatus()).isBetween(400, 499);
        assertThat(delivered.get()).isNull();
    }

    @Test
    void passwordResetSucceedsAndRejectsInvalidOrExpiredTokens() throws Exception {
        var token = tokens.issueForUser(USER_ID).rawToken();
        var success = mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"NewPassword#123\"}"))
                .andReturn().getResponse();
        assertThat(success.getStatus()).isEqualTo(204);
        assertThat(storedHash.get()).startsWith("$argon2id$").doesNotContain("NewPassword#123");

        var invalid = mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid-token\",\"newPassword\":\"NewPassword#123\"}"))
                .andReturn().getResponse();
        assertThat(invalid.getStatus()).isEqualTo(400);

        var expiredToken = tokens.issueForUser(USER_ID).rawToken();
        var expiredTokens = new PasswordRecoveryTokenService(repository,
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC),
                PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(15)));
        var expiredReset = new PasswordResetService(expiredTokens,
                (PasswordHasher) new Argon2PasswordEncoder(16, 32, 1, 19_456, 2)::encode,
                (userId, hash) -> storedHash.set(hash), userId -> { });
        var expiredMvc = MockMvcBuilders.standaloneSetup(new PasswordRecoveryController(
                new PasswordRecoveryRequestService(email -> Optional.empty(), expiredTokens, (email, deliveryToken, expiry) -> { }),
                expiredReset)).setControllerAdvice(new ApiExceptionHandler()).build();
        var expired = expiredMvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + expiredToken + "\",\"newPassword\":\"NewPassword#123\"}"))
                .andReturn().getResponse();
        assertThat(expired.getStatus()).isEqualTo(400);
    }
}
