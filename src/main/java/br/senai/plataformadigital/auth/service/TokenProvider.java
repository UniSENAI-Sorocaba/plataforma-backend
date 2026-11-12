package br.senai.plataformadigital.auth.service;

import br.senai.plataformadigital.auth.domain.User;
import br.senai.plataformadigital.auth.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class TokenProvider {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public TokenProvider(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.issuer:plataforma-digital}") String issuer
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    public TokenResponse generateTokens(User user) {
        Instant issuedAt = Instant.now();
        String accessToken = encode(user, issuedAt, ACCESS_TOKEN_TTL, "access");
        String refreshToken = encode(user, issuedAt, REFRESH_TOKEN_TTL, "refresh");

        return new TokenResponse(
                accessToken,
                refreshToken,
                ACCESS_TOKEN_TTL.toSeconds(),
                "Bearer"
        );
    }

    private String encode(User user, Instant issuedAt, Duration ttl, String tokenType) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .claim("role", user.role())
                .claim("token_type", tokenType)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
