package br.senai.plataformadigital.auth;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
    Optional<AuthenticatedUser> findByEmail(String email);

    default Optional<AuthenticatedUser> findById(UUID id) {
        return Optional.empty();
    }
}
