package br.senai.plataformadigital.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String role,
        boolean active,
        boolean blocked
) {
    public User {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", role=" + role
                + ", active=" + active + ", blocked=" + blocked + "]";
    }
}
