package br.senai.plataformadigital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CredentialServiceTest {
    @Autowired
    private CredentialService credentialService;

    @Test
    void hashesWithArgon2idAndMatchesOnlyTheOriginalPassword() {
        String hash = credentialService.hash("senha-de-teste-segura1");

        assertThat(hash).startsWith("$argon2id$");
        assertThat(hash).doesNotContain("senha-de-teste-segura");
        assertThat(credentialService.matches("senha-de-teste-segura1", hash)).isTrue();
        assertThat(credentialService.matches("senha-incorreta", hash)).isFalse();
    }

    @Test
    void generatesDifferentSaltForTheSamePassword() {
        String firstHash = credentialService.hash("senha-de-teste-segura1");
        String secondHash = credentialService.hash("senha-de-teste-segura1");

        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(credentialService.matches("senha-de-teste-segura1", secondHash)).isTrue();
    }

    @Test
    void rejectsBlankPasswordWhenCreatingCredential() {
        assertThatIllegalArgumentException().isThrownBy(() -> credentialService.hash("  "));
    }
}
