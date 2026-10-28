package br.senai.plataformadigital.auth.api.authorization;

import java.util.Objects;
import java.util.Optional;

/**
 * Orquestra identidade confiável e política explícita, sem concessão implícita.
 * A integração HTTP é registrada por AuthorizationConfiguration; a política
 * de permissões permanece uma porta substituível, com negação por padrão.
 */
public final class AuthorizationService {
    private final AuthenticatedSubjectProvider subjects;
    private final AuthorizationPolicy policy;

    public AuthorizationService(AuthenticatedSubjectProvider subjects, AuthorizationPolicy policy) {
        this.subjects = Objects.requireNonNull(subjects, "Provedor de identidade obrigatório");
        this.policy = Objects.requireNonNull(policy, "Política obrigatória");
    }

    public AuthorizationDecision authorize(AuthorizationRequest request) {
        return evaluate(request).decision();
    }

    public record Evaluation(AuthorizationDecision decision, Optional<AuthenticatedSubject> subject) { }

    public Evaluation evaluate(AuthorizationRequest request) {
        Objects.requireNonNull(request, "Requisição de autorização obrigatória");
        Optional<AuthenticatedSubject> subject = Optional.empty();
        try {
            subject = subjects.currentSubject();
            if (subject == null) {
                return new Evaluation(AuthorizationDecision.UNAVAILABLE, Optional.empty());
            }
            if (subject.isEmpty()) {
                return new Evaluation(AuthorizationDecision.UNAUTHENTICATED, subject);
            }
            return new Evaluation(policy.permits(subject.orElseThrow(), request)
                    ? AuthorizationDecision.ALLOWED : AuthorizationDecision.DENIED, subject);
        } catch (RuntimeException unavailable) {
            // Nunca converter falha da fonte em concessão ou expor sua mensagem ao consumidor.
            return new Evaluation(AuthorizationDecision.UNAVAILABLE, subject);
        }
    }
}
