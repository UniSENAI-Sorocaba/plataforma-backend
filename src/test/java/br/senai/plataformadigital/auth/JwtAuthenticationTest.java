package br.senai.plataformadigital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

class JwtAuthenticationTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AuthenticationService authenticationService;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAKey key = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) pair.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) pair.getPrivate()).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        decoder = NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) pair.getPublic()).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator));

        PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String hash = passwordEncoder.encode("senha-segura");
        UserAccountRepository repository = email -> email.equals("aluno@example.edu")
                ? Optional.of(new AuthenticatedUser(USER_ID, email, hash, "STUDENT", true))
                : Optional.empty();
        JwtTokenService tokens = new JwtTokenService(encoder, Clock.fixed(NOW, ZoneOffset.UTC),
                "https://plataforma-digital.local", "plataforma-api");
        authenticationService = new AuthenticationService(repository, passwordEncoder, tokens);
    }

    @Test
    void validLoginEmitsRs256JwtWithRequiredClaims() {
        LoginResponse response = authenticationService.authenticate("aluno@example.edu", "senha-segura");

        Jwt jwt = decoder.decode(response.accessToken());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://plataforma-digital.local");
        assertThat(jwt.getAudience()).containsExactly("plataforma-api");
        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void invalidEmailAndPasswordHaveTheSameFailure() {
        assertThatThrownBy(() -> authenticationService.authenticate("unknown@example.edu", "senha-segura"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authenticationService.authenticate("aluno@example.edu", "errada"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
