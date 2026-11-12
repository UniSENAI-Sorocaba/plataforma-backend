package br.senai.plataformadigital.auth.api.authorization;

/** Ponte pública para políticas de módulos. Deve existir exatamente um responsável por ação. */
public interface AuthorizationPolicyContribution {
    boolean supports(String action);
    boolean permits(AuthenticatedSubject subject, AuthorizationRequest request);
}
