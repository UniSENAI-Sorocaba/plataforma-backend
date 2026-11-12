package br.senai.plataformadigital.auth.repository;

import br.senai.plataformadigital.auth.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
}
