package br.senai.plataformadigital.auth.api.authorization;

import java.util.Objects;
import java.util.Optional;

/**
 * Alvo de uma verificação. Vocabulário de ações/recursos/escopos depende da matriz aprovada.
 * Scope ausente não significa acesso global; a política deve decidir explicitamente.
 * A identidade do solicitante é obtida separadamente, nunca deste objeto.
 */
public record AuthorizationRequest(String action, String resource, Optional<String> scope) {
    public AuthorizationRequest {
        Objects.requireNonNull(action, "Ação obrigatória");
        Objects.requireNonNull(resource, "Recurso obrigatório");
        Objects.requireNonNull(scope, "Contêiner de escopo obrigatório");
        if (action.isBlank()) {
            throw new IllegalArgumentException("Ação vazia");
        }
        if (resource.isBlank()) {
            throw new IllegalArgumentException("Recurso vazio");
        }
        if (scope.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException("Escopo vazio");
        }
    }
}
