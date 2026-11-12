package br.senai.plataformadigital.auth.api.authorization;

import java.util.Objects;

/** Identificador opaco de identidade já autenticada pelo servidor; não recebe tokens. */
public record AuthenticatedSubject(String id) {
    public AuthenticatedSubject {
        Objects.requireNonNull(id, "Identificador de identidade obrigatório");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Identificador de identidade vazio");
        }
    }
}
