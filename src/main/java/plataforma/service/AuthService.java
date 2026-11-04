package plataforma.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.exception.ResourceNotFoundException;
import plataforma.model.RefreshToken;
import plataforma.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final RefreshTokenRepository tokenRepository;

    public AuthService(RefreshTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash do token", e);
        }
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        
        RefreshToken storedToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token inválido ou não encontrado."));

        if (storedToken.isUsed()) {
            tokenRepository.revokeFamilyByFamilyId(storedToken.getFamilyId());
            throw new SecurityException("Detectado reuso de token obsoleto! Sessão revogada.");
        }

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new SecurityException("Sessão expirada ou revogada.");
        }

        storedToken.setUsed(true);
        tokenRepository.save(storedToken);

        String newRawToken = UUID.randomUUID().toString();
        RefreshToken newToken = new RefreshToken();
        newToken.setTokenHash(hashToken(newRawToken));
        newToken.setFamilyId(storedToken.getFamilyId());
        newToken.setUsername(storedToken.getUsername());
        newToken.setExpiresAt(Instant.now().plusSeconds(86400));
        
        tokenRepository.save(newToken);

        return newRawToken;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        
        String tokenHash = hashToken(rawToken);
        tokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            tokenRepository.revokeFamilyByFamilyId(token.getFamilyId());
        });
    }
}