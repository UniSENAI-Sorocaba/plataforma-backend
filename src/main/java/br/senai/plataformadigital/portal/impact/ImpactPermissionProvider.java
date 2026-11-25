package br.senai.plataformadigital.portal.impact;

import java.util.UUID;

/**
 * Porta para autorização server-side. O adapter resolve permissões e escopo.
 * A matriz é aprovada em PG-006 e os vínculos persistidos pertencem à DB-006;
 * não inferir papéis a partir de headers do cliente ou criar permissões de fallback.
 */
@FunctionalInterface
public interface ImpactPermissionProvider {
    boolean permits(String authenticatedActor, Impact.Action action, UUID impactId);
}
