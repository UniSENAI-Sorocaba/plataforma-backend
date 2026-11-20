package br.senai.plataformadigital.auth;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class JwtConfiguration {
    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt.private-key}") String privateKey,
            @Value("${security.jwt.public-key}") String publicKey) throws Exception {
        RSAKey key = rsaKey(privateKey, publicKey);
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.public-key}") String publicKey) throws Exception {
        return NimbusJwtDecoder.withPublicKey(readPublicKey(publicKey)).build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/first-access",
                                "/api/v1/auth/password-recovery/request",
                                "/api/v1/auth/password-recovery/reset",
                                "/api/v1/deliverables/*/content",
                                "/api/v1/deliverables/*/download",
                                "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder)))
                .build();
    }

    @Bean
    JwtTokenService jwtTokenService(JwtEncoder encoder, Clock clock,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience) {
        return new JwtTokenService(encoder, clock, issuer, audience);
    }

    private RSAKey rsaKey(String privatePem, String publicPem) throws Exception {
        RSAPrivateKey privateKey = readPrivateKey(privatePem);
        RSAPublicKey publicKey = readPublicKey(publicPem);
        if (publicKey.getModulus().bitLength() < 2048 || !publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("As chaves JWT devem formar um par RSA de pelo menos 2048 bits.");
        }
        return new RSAKey.Builder(publicKey).privateKey(privateKey).algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).build();
    }

    private RSAPublicKey readPublicKey(String pem) throws Exception {
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(decode(pem, "PUBLIC KEY")));
    }

    private RSAPrivateKey readPrivateKey(String pem) throws Exception {
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(decode(pem, "PRIVATE KEY")));
    }

    private byte[] decode(String pem, String type) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Chave JWT ausente.");
        }
        String encoded = pem.replace("\\n", "\n")
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
