package br.senai.plataformadigital.auth.api.authorization;

import java.util.Optional;

/**
 * Porta para o contexto autenticado do servidor. Não confiar em subject, papéis
 * ou permissões informados em body, query ou headers arbitrários do cliente.
 * A integração com BE-004 deve validar autenticação antes de fornecer identidade.
 */
@FunctionalInterface
public interface AuthenticatedSubjectProvider {
    Optional<AuthenticatedSubject> currentSubject();
}
