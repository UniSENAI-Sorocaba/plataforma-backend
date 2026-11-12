package br.senai.plataformadigital.auth.service;

import br.senai.plataformadigital.auth.domain.User;
import br.senai.plataformadigital.auth.dto.LoginRequest;
import br.senai.plataformadigital.auth.dto.TokenResponse;
import br.senai.plataformadigital.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Credenciais inválidas";

    private final UserRepository userRepository;
    private final Argon2PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            Argon2PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public TokenResponse login(LoginRequest request, String correlationId) {
        LOGGER.info(
                "Login attempt email={} correlationId={}",
                sanitizeForLog(request.email()),
                sanitizeForLog(correlationId)
        );

        Optional<User> user = userRepository.findByEmail(request.email());
        String passwordHash = user.map(User::passwordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

        if (user.isEmpty() || !user.get().active() || user.get().blocked() || !passwordMatches) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return tokenProvider.generateTokens(user.get());
    }

    private String sanitizeForLog(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9@._:+-]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 128));
    }
}
