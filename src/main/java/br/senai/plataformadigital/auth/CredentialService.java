package br.senai.plataformadigital.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CredentialService {
    private final PasswordEncoder passwordEncoder;

    public CredentialService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPassword) {
        requirePassword(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    private void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser vazia.");
        }
    }
}
