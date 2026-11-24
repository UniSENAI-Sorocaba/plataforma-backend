package br.senai.plataformadigital.auth;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP boundary for rotating and revoking opaque refresh-token sessions. */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class SessionController {
    private final RefreshTokenService refreshTokens;
    private final JwtTokenService accessTokens;
    private final UserAccountRepository users;

    public SessionController(RefreshTokenService refreshTokens, JwtTokenService accessTokens,
            UserAccountRepository users) {
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.users = users;
    }

    @PostMapping("/refresh")
    ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        IssuedRefreshToken rotated = refreshTokens.rotate(request.refreshToken());
        AuthenticatedUser user = users.findById(rotated.userId()).orElseThrow(InvalidRefreshTokenException::new);
        LoginResponse access = accessTokens.issue(user);
        return ResponseEntity.ok(new LoginResponse(access.accessToken(), access.tokenType(), access.expiresIn(),
                rotated.rawToken()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.revokeToken(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<Map<String, Object>> invalidRefreshToken() {
        return ResponseEntity.status(401).body(Map.of(
                "type", "about:blank",
                "title", "Unauthorized",
                "status", 401,
                "detail", "Refresh token inválido ou expirado."));
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }
}
