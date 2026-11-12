package br.senai.plataformadigital.auth.api.authorization;

/** Porta para auditoria transversal. Não recebe token, body, URI concreta ou mensagens privadas. */
@FunctionalInterface
public interface AuthorizationAudit {
    void record(Event event);

    record Event(String method, String route, String action, String actor,
                 AuthorizationDecision decision, String correlationId) { }
}
