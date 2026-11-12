package br.senai.plataformadigital.auth.service;

import br.senai.plataformadigital.auth.domain.User;
import br.senai.plataformadigital.auth.dto.TokenResponse;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenProviderTest {
    @Test
    void issuesSignedAccessAndRefreshTokensWithTheirApprovedClaims() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        JwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        TokenProvider tokenProvider = new TokenProvider(encoder, "https://auth.senai.br");
        User user = new User(UUID.randomUUID(), "aluno@senai.br", "encoded", "STUDENT", true, false);

        Instant beforeIssue = Instant.now();
        TokenResponse response = tokenProvider.generateTokens(user);
        Jwt accessToken = decoder.decode(response.accessToken());
        Jwt refreshToken = decoder.decode(response.refreshToken());

        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("https://auth.senai.br", accessToken.getIssuer().toString());
        assertEquals(user.id().toString(), accessToken.getSubject());
        assertEquals("STUDENT", accessToken.getClaimAsString("role"));
        assertEquals("access", accessToken.getClaimAsString("token_type"));
        assertEquals("refresh", refreshToken.getClaimAsString("token_type"));
        assertEquals(Duration.ofMinutes(15), Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        assertEquals(Duration.ofDays(7), Duration.between(refreshToken.getIssuedAt(), refreshToken.getExpiresAt()));
        assertTrue(accessToken.getIssuedAt().isAfter(beforeIssue.minusSeconds(1)));
    }
}
