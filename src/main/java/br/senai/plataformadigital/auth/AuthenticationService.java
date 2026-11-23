package br.senai.plataformadigital.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
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

    public LoginResponse authenticate(String identifier, String password) {
        String normalized = identifier == null ? "" : identifier.trim();
        String cpfDigits = normalized.replaceAll("\\D", "");
        Optional<AuthenticatedUser> user;
        if (normalized.contains("@")) {
            user = repository.findByEmail(normalized.toLowerCase());
        } else if (cpfDigits.length() == 11 && isValidCpf(cpfDigits)) {
            user = repository.findByCpf(cpfDigits);
        } else {
            user = Optional.empty();
        }
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

    private boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int first = checkDigit(cpf, 9);
        int second = checkDigit(cpf, 10);
        return first == Character.digit(cpf.charAt(9), 10)
                && second == Character.digit(cpf.charAt(10), 10);
    }

    private int checkDigit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (length + 1 - index);
        }
        int digit = 11 - (sum % 11);
        return digit >= 10 ? 0 : digit;
    }
}
