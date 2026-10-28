package br.senai.plataformadigital.auth.api.authorization;

import java.util.Objects;
import java.util.Optional;

/**
 * Orquestra identidade confiável e política explícita, sem concessão implícita.
 * Não é bean automático e não instala filtros nem altera endpoints existentes.
 */
public final class AuthorizationService {
    private final AuthenticatedSubjectProvider subjects;
    private final AuthorizationPolicy policy;

    public AuthorizationService(AuthenticatedSubjectProvider subjects, AuthorizationPolicy policy) {
        this.subjects = Objects.requireNonNull(subjects, "Provedor de identidade obrigatório");
        this.policy = Objects.requireNonNull(policy, "Política obrigatória");
    }

    public AuthorizationDecision authorize(AuthorizationRequest request) {
        Objects.requireNonNull(request, "Requisição de autorização obrigatória");
        try {
            Optional<AuthenticatedSubject> subject = subjects.currentSubject();
            if (subject == null) {
                return AuthorizationDecision.UNAVAILABLE;
            }
            if (subject.isEmpty()) {
                return AuthorizationDecision.UNAUTHENTICATED;
            }
            return policy.permits(subject.orElseThrow(), request)
                    ? AuthorizationDecision.ALLOWED : AuthorizationDecision.DENIED;
        } catch (RuntimeException unavailable) {
            // Nunca converter falha da fonte em concessão ou expor sua mensagem ao consumidor.
            return AuthorizationDecision.UNAVAILABLE;
        }
    }
}
