package br.senai.plataformadigital.auth.service;

import br.senai.plataformadigital.auth.domain.User;
import br.senai.plataformadigital.auth.dto.LoginRequest;
import br.senai.plataformadigital.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceEnumerationTest {
    private static final String UNKNOWN_EMAIL = "inexistente@senai.br";
    private static final String VALID_EMAIL = "valido@senai.br";
    private static final String INACTIVE_EMAIL = "inativo@senai.br";
    private static final String CORRECT_PASSWORD = "senha-correta-de-teste";
    private static final String INCORRECT_PASSWORD = "senha-incorreta-de-teste";
    private static final String CORRELATION_ID = "enumeration-test";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    private Argon2PasswordEncoder passwordEncoder;
    private AuthService authService;
    private User validUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 2);
        String passwordHash = passwordEncoder.encode(CORRECT_PASSWORD);

        validUser = new User(
                UUID.randomUUID(),
                VALID_EMAIL,
                passwordHash,
                "STUDENT",
                true,
                false
        );
        inactiveUser = new User(
                UUID.randomUUID(),
                INACTIVE_EMAIL,
                passwordHash,
                "STUDENT",
                false,
                false
        );

        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return switch (email) {
                case UNKNOWN_EMAIL -> Optional.empty();
                case VALID_EMAIL -> Optional.of(validUser);
                case INACTIVE_EMAIL -> Optional.of(inactiveUser);
                default -> Optional.empty();
            };
        });

        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void preventEnumeration_timingMustBeSimilar() {
        for (int attempt = 0; attempt < 3; attempt++) {
            rejectLogin(UNKNOWN_EMAIL, INCORRECT_PASSWORD);
            rejectLogin(VALID_EMAIL, INCORRECT_PASSWORD);
        }

        long unknownEmailTime = measureFailedLogin(UNKNOWN_EMAIL, INCORRECT_PASSWORD);
        long validEmailTime = measureFailedLogin(VALID_EMAIL, INCORRECT_PASSWORD);

        long difference = Math.abs(unknownEmailTime - validEmailTime);
        assertTrue(
                difference < 50,
                () -> "Latências de falha diferiram por " + difference
                        + " ms (e-mail inexistente: " + unknownEmailTime
                        + " ms, e-mail válido: " + validEmailTime + " ms)"
        );
    }

    @Test
    void rejectsUnknownEmailWithExactInvalidCredentialsMessage() {
        assertInvalidCredentials(UNKNOWN_EMAIL, INCORRECT_PASSWORD);
    }

    @Test
    void rejectsInactiveUserWithExactInvalidCredentialsMessage() {
        assertInvalidCredentials(INACTIVE_EMAIL, CORRECT_PASSWORD);
    }

    @Test
    void rejectsIncorrectPasswordWithExactInvalidCredentialsMessage() {
        assertInvalidCredentials(VALID_EMAIL, INCORRECT_PASSWORD);
    }

    private long measureFailedLogin(String email, String password) {
        long start = System.nanoTime();
        rejectLogin(email, password);
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private void assertInvalidCredentials(String email, String password) {
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(email, password), CORRELATION_ID)
        );
        assertEquals("Credenciais inválidas", exception.getMessage());
    }

    private void rejectLogin(String email, String password) {
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(email, password), CORRELATION_ID)
        );
    }
}
