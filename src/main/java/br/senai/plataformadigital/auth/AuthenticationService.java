package br.senai.plataformadigital.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({UserAccountRepository.class, JwtTokenService.class})
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class AuthenticationService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final String dummyHash;

    public AuthenticationService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse authenticate(String email, String password) {
        Optional<AuthenticatedUser> user = repository.findByEmail(email);
        String hash = user.map(AuthenticatedUser::passwordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !user.get().active() || !matches) {
            throw new InvalidCredentialsException();
        }
        return tokenService.issue(user.get());
    }
}
