package br.senai.plataformadigital.auth.api.authorization;

/** Resultado técnico sem tokens, dados pessoais ou mensagens da fonte de permissões. */
public enum AuthorizationDecision {
    ALLOWED,
    UNAUTHENTICATED,
    DENIED,
    UNAVAILABLE;

    public boolean allowsAccess() {
        return this == ALLOWED;
    }
}
