package br.senai.plataformadigital.portal.impact;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationPolicyContribution;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest;
import java.util.UUID;

/** Ponte para o interceptor BE-010; reutiliza a mesma fonte de permissões do fluxo de impacto. */
public final class ImpactAuthorizationContribution implements AuthorizationPolicyContribution {
    private final ImpactPermissionProvider permissions;

    public ImpactAuthorizationContribution(ImpactPermissionProvider permissions) { this.permissions = permissions; }

    @Override public boolean supports(String action) { return action != null && action.startsWith("impact:"); }

    @Override public boolean permits(AuthenticatedSubject subject, AuthorizationRequest request) {
        if (!supports(request.action()) || request.scope().isPresent()) return false;
        try {
            Impact.Action action = Impact.Action.valueOf(request.action().substring("impact:".length()));
            UUID id;
            if (action == Impact.Action.CREATE) {
                if (!request.resource().equals("impacts")) return false;
                id = null;
            } else {
                id = UUID.fromString(request.resource());
            }
            return permissions.permits(subject.id(), action, id);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
