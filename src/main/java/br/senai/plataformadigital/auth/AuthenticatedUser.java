package br.senai.plataformadigital.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String passwordHash, String role, boolean active, String fullName) {
    public AuthenticatedUser(UUID id, String email, String passwordHash, String role, boolean active) {
        this(id, email, passwordHash, role, active, "");
    }
}
