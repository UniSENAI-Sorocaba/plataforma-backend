package plataforma.auth;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.repository.UserAccountRepository;

@Service
@Transactional
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class AuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String INVALID_CREDENTIALS = "Credenciais inválidas";

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationAttemptLimiter attemptLimiter;
    private final String dummyHash;

    public AuthenticationService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            JwtTokenService jwtTokenService,
            AuthenticationAttemptLimiter attemptLimiter
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenService = jwtTokenService;
        this.attemptLimiter = attemptLimiter;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    public LoginResponse authenticate(LoginRequest request, String remoteAddress) {
        try {
            attemptLimiter.checkAllowed(request.cpf(), remoteAddress);
        } catch (AuthenticationRateLimitException exception) {
            LOGGER.info("Authentication outcome=rate_limited");
            throw exception;
        }

        try {
            LoginResponse response = authenticateCredentials(request);
            attemptLimiter.recordSuccess(request.cpf());
            LOGGER.info("Authentication outcome=success");
            return response;
        } catch (BadCredentialsException exception) {
            attemptLimiter.recordFailure(request.cpf(), remoteAddress);
            LOGGER.info("Authentication outcome=failed");
            throw exception;
        }
    }

    private LoginResponse authenticateCredentials(LoginRequest request) {
        Optional<AuthenticatedAccount> account = userAccountRepository.findByCpf(request.cpf());
        String storedHash = account.map(AuthenticatedAccount::passwordHash).orElse(null);
        String encodedHash = passwordHasher.supportsHash(storedHash) ? storedHash : dummyHash;
        boolean passwordMatches = passwordHasher.verify(request.password(), encodedHash);

        if (account.isEmpty()
                || !"active".equals(account.get().status())
                || account.get().passwordHash() == null
                || account.get().role() == null
                || account.get().role().isBlank()
                || !passwordMatches) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (passwordHasher.needsRehash(encodedHash)
                && userAccountRepository.updatePasswordHash(
                        account.get().id(),
                        encodedHash,
                        passwordHasher.hash(request.password())) != 1) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return jwtTokenService.issue(account.get());
    }
}
