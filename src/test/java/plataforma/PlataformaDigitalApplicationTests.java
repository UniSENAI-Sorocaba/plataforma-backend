package plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import plataforma.auth.passwordrecovery.InMemoryPasswordRecoveryTokenRepository;
import plataforma.auth.passwordrecovery.PasswordRecoveryTokenRepository;
import plataforma.auth.passwordrecovery.PasswordRecoveryTokenService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PlataformaDigitalApplicationTests {
    @Autowired
    private PasswordRecoveryTokenService passwordRecoveryTokenService;

    @Autowired
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Test
    void contextLoads() {
        assertThat(passwordRecoveryTokenService).isNotNull();
        assertThat(passwordRecoveryTokenRepository).isInstanceOf(InMemoryPasswordRecoveryTokenRepository.class);
    }
}
