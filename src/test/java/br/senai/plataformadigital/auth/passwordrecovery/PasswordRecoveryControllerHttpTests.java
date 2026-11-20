package br.senai.plataformadigital.auth.passwordrecovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;

import br.senai.plataformadigital.auth.AuthenticatedUser;
import br.senai.plataformadigital.auth.CredentialService;
import br.senai.plataformadigital.auth.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PasswordRecoveryControllerHttpTests {
    private MockMvc mvc;
    private InMemoryPasswordRecoveryTokenRepository repository;
    private PasswordRecoveryTokenService tokens;
    private CapturingDelivery delivery;
    private MutableAccounts accounts;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPasswordRecoveryTokenRepository();
        tokens = new PasswordRecoveryTokenService(repository, Clock.systemUTC(),
                PasswordRecoveryTokenPolicy.withLifetime(Duration.ofMinutes(30)));
        delivery = new CapturingDelivery();
        accounts = new MutableAccounts();
        PasswordRecoveryRequestService requests = new PasswordRecoveryRequestService(
                subject -> true, tokens, delivery);
        ObjectProvider<PasswordRecoveryRequestService> provider = new FixedProvider<>(requests);
        mvc = MockMvcBuilders.standaloneSetup(new PasswordRecoveryController(tokens, accounts,
                new CredentialService(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()), provider)).build();
    }

    @Test
    void requestIsAcceptedAndResetTokenCanOnlyBeConsumedOnce() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@unisenai.edu.br\"}"))
                .andExpect(status().isAccepted());
        assertThat(delivery.rawToken).isNotBlank();

        mvc.perform(post("/api/v1/auth/password-recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + delivery.rawToken + "\",\"password\":\"novaSenha123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/password-recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + delivery.rawToken + "\",\"password\":\"outraSenha123\"}"))
                .andExpect(status().isBadRequest());
        assertThat(accounts.passwordHash).startsWith("$argon2id$");
    }

    private static final class CapturingDelivery implements PasswordRecoveryTokenDelivery {
        private String rawToken;
        public void deliver(String subject, String rawToken, java.time.Instant expiresAt) { this.rawToken = rawToken; }
    }

    private static final class MutableAccounts implements UserAccountRepository {
        private String passwordHash;
        public Optional<AuthenticatedUser> findByEmail(String email) { return Optional.empty(); }
        public boolean updatePasswordByEmail(String email, String passwordHash) { this.passwordHash = passwordHash; return true; }
    }

    private record FixedProvider<T>(T value) implements ObjectProvider<T> {
        public T getObject(Object... args) { return value; }
        public T getIfAvailable() { return value; }
        public T getIfUnique() { return value; }
        public java.util.stream.Stream<T> orderedStream() { return java.util.stream.Stream.of(value); }
    }
}
