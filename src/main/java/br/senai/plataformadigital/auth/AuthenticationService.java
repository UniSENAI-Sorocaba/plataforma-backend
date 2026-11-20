package br.senai.plataformadigital.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({UserAccountRepository.class, JwtTokenService.class})
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class AuthenticationService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final String dummyHash;

    public AuthenticationService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService) {
        this(repository, passwordEncoder, tokenService, (RefreshTokenService) null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthenticationService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService, ObjectProvider<RefreshTokenService> refreshTokens) {
        this(repository, passwordEncoder, tokenService, refreshTokens.getIfAvailable());
    }

    private AuthenticationService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService, RefreshTokenService refreshTokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse authenticate(String email, String password) {
        Optional<AuthenticatedUser> user = repository.findByEmail(email);
        String hash = user.map(AuthenticatedUser::passwordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !user.get().active() || !matches) {
            throw new InvalidCredentialsException();
        }
        LoginResponse access = tokenService.issue(user.get());
        if (refreshTokenService == null) {
            return access;
        }
        IssuedRefreshToken refresh = refreshTokenService.issue(user.get().id());
        return new LoginResponse(access.accessToken(), access.tokenType(), access.expiresIn(), refresh.rawToken());
    }
}
