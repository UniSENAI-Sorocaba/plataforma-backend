package plataforma.auth;

import org.springframework.security.core.AuthenticationException;

public final class AuthenticationRateLimitException extends AuthenticationException {
    public AuthenticationRateLimitException() {
        super("Credenciais inválidas");
    }
}
