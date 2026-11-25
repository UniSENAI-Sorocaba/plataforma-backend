package br.senai.plataformadigital.portal.impact;

import br.senai.plataformadigital.auth.api.authorization.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImpactAuthorizationContributionTests {
    @Test void bridgeMapsActionAndResourceToTheExistingServerPolicy() {
        UUID id = UUID.randomUUID();
        var subject = new AuthenticatedSubject("synthetic-reader");
        var contribution = new ImpactAuthorizationContribution((actor, action, target) ->
                actor.equals(subject.id()) && action == Impact.Action.READ && id.equals(target));
        assertTrue(contribution.permits(subject, new AuthorizationRequest("impact:READ", id.toString(), Optional.empty())));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("impact:AUDIT", id.toString(), Optional.empty())));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("other:READ", id.toString(), Optional.empty())));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("impact:READ", id.toString(), Optional.of("unapproved-scope"))));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("impact:READ", "invalid-uuid", Optional.empty())));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("impact:INVALID", id.toString(), Optional.empty())));
        assertFalse(contribution.supports(null));
    }

    @Test void createRequiresTheExactFixedResourceAndPassesNullTargetToTheServerPolicy() {
        var contribution = new ImpactAuthorizationContribution((actor, action, id) -> action == Impact.Action.CREATE && id == null);
        var subject = new AuthenticatedSubject("synthetic-editor");
        assertTrue(contribution.permits(subject, new AuthorizationRequest("impact:CREATE", "impacts", Optional.empty())));
        assertFalse(contribution.permits(subject, new AuthorizationRequest("impact:CREATE", "other", Optional.empty())));
    }
}
