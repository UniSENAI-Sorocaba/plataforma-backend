package br.senai.plataformadigital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class FirstAccessServiceTest {
    @Test
    void activatesPendingAccountWithHashedPasswordAndRejectsSecondActivation() {
        Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        CredentialService credentials = new CredentialService(encoder);
        class Store implements UserAccountRepository {
            boolean activated;
            String hash;
            public Optional<AuthenticatedUser> findByEmail(String email) { return Optional.empty(); }
            public boolean activateFirstAccess(String cpf, String passwordHash) {
                if (activated) return false;
                activated = true;
                hash = passwordHash;
                return true;
            }
        }
        Store store = new Store();
        FirstAccessService service = new FirstAccessService(store, credentials);

        service.activate("52998224725", "senhaSegura123");

        assertThat(store.hash).startsWith("$argon2id$");
        assertThat(credentials.matches("senhaSegura123", store.hash)).isTrue();
        assertThatThrownBy(() -> service.activate("52998224725", "outraSenha123"))
                .isInstanceOf(InvalidFirstAccessException.class);
    }
}
