package br.senai.plataformadigital.auth.service;

import br.senai.plataformadigital.auth.domain.User;
import br.senai.plataformadigital.auth.dto.LoginRequest;
import br.senai.plataformadigital.auth.dto.TokenResponse;
import br.senai.plataformadigital.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private static final String EMAIL = "aluno@senai.br";
    private static final String PASSWORD = "senha-de-teste";

    private UserRepository userRepository;
    private Argon2PasswordEncoder passwordEncoder;
    private TokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(Argon2PasswordEncoder.class);
        tokenProvider = mock(TokenProvider.class);
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void rejectsUnknownEmailAfterCheckingDummyHash() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(PASSWORD, "dummy-hash")).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD), "correlation")
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(passwordEncoder).matches(PASSWORD, "dummy-hash");
    }

    @Test
    void rejectsInactiveUserWithTheSameCredentialsError() {
        User inactiveUser = user(false, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches(PASSWORD, inactiveUser.passwordHash())).thenReturn(true);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD), "correlation")
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(passwordEncoder).matches(PASSWORD, inactiveUser.passwordHash());
    }

    @Test
    void rejectsIncorrectPasswordWithTheSameCredentialsError() {
        User activeUser = user(true, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(PASSWORD, activeUser.passwordHash())).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD), "correlation")
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
    }

    @Test
    void rejectsBlockedUserWithTheSameCredentialsError() {
        User blockedUser = user(true, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(blockedUser));
        when(passwordEncoder.matches(PASSWORD, blockedUser.passwordHash())).thenReturn(true);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest(EMAIL, PASSWORD), "correlation")
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
    }

    @Test
    void returnsTokensForAnActiveUserWithTheCorrectPassword() {
        User activeUser = user(true, false);
        TokenResponse expected = new TokenResponse("access", "refresh", 900, "Bearer");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(PASSWORD, activeUser.passwordHash())).thenReturn(true);
        when(tokenProvider.generateTokens(activeUser)).thenReturn(expected);

        TokenResponse actual = authService.login(new LoginRequest(EMAIL, PASSWORD), "correlation");

        assertEquals(expected, actual);
    }

    @Test
    void loginRequestToStringDoesNotExposePassword() {
        String formatted = new LoginRequest(EMAIL, PASSWORD).toString();

        assertFalse(formatted.contains(PASSWORD));
    }

    private User user(boolean active, boolean blocked) {
        return new User(UUID.randomUUID(), EMAIL, "encoded-password", "STUDENT", active, blocked);
    }
}
