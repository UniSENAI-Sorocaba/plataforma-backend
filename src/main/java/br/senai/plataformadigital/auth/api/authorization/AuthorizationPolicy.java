package br.senai.plataformadigital.auth.api.authorization;

/**
 * Porta da política aprovada. Somente true autoriza. A implementação futura
 * resolve papéis, permissões e vínculos de escopo em fontes confiáveis.
 * Esta interface não determina união de papéis, hierarquia, wildcards ou cache.
 */
@FunctionalInterface
public interface AuthorizationPolicy {
    boolean permits(AuthenticatedSubject subject, AuthorizationRequest request);
}
