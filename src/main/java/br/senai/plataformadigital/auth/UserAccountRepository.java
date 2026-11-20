package br.senai.plataformadigital.auth;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
    Optional<AuthenticatedUser> findByEmail(String email);

    default Optional<AuthenticatedUser> findById(UUID id) {
        return Optional.empty();
    }

    default Optional<AuthenticatedUser> findByCpf(String cpf) {
        return Optional.empty();
    }

    default boolean activateFirstAccess(String cpf, String passwordHash) {
        return false;
    }

    default boolean updatePasswordByEmail(String email, String passwordHash) {
        return false;
    }
}
