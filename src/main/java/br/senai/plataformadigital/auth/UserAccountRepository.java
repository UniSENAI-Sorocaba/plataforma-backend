package br.senai.plataformadigital.auth;

import java.util.Optional;

public interface UserAccountRepository {
    Optional<AuthenticatedUser> findByEmail(String email);
}
