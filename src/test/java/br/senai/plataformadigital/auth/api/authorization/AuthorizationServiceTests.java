package br.senai.plataformadigital.auth.api.authorization;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationServiceTests {
    private static final AuthenticatedSubject SUBJECT = new AuthenticatedSubject("synthetic-subject");
    private static final AuthorizationRequest REQUEST =
            new AuthorizationRequest("synthetic:read", "synthetic-resource", Optional.of("synthetic-scope"));

    @Test
    void anonymousSubjectIsNotPassedToPolicy() {
        AuthorizationService service = new AuthorizationService(Optional::empty, (subject, request) -> {
            throw new AssertionError("Identidade ausente não deve consultar política");
        });
        assertEquals(AuthorizationDecision.UNAUTHENTICATED, service.authorize(REQUEST));
    }

    @Test
    void onlyExplicitPolicyPermissionAllowsAccess() {
        AuthorizationService service = new AuthorizationService(() -> Optional.of(SUBJECT), (subject, request) -> {
            assertEquals(SUBJECT, subject);
            assertEquals(REQUEST, request);
            return true;
        });
        assertEquals(AuthorizationDecision.ALLOWED, service.authorize(REQUEST));
    }

    @Test
    void policyDenialRemainsDenied() {
        AuthorizationService service = new AuthorizationService(() -> Optional.of(SUBJECT), (subject, request) -> false);
        assertEquals(AuthorizationDecision.DENIED, service.authorize(REQUEST));
    }

    @Test
    void subjectProviderFailureDoesNotGrantOrLeakItsMessage() {
        AuthorizationService service = new AuthorizationService(() -> {
            throw new IllegalStateException("synthetic-secret-not-for-client");
        }, (subject, request) -> true);
        AuthorizationDecision decision = service.authorize(REQUEST);
        assertEquals(AuthorizationDecision.UNAVAILABLE, decision);
        assertFalse(decision.toString().contains("synthetic-secret"));
    }

    @Test
    void policyFailureDoesNotGrantOrLeakItsMessage() {
        AuthorizationService service = new AuthorizationService(() -> Optional.of(SUBJECT), (subject, request) -> {
            throw new IllegalStateException("synthetic-secret-not-for-client");
        });
        assertEquals(AuthorizationDecision.UNAVAILABLE, service.authorize(REQUEST));
    }

    @Test
    void invalidProviderResultDoesNotGrant() {
        AuthorizationService service = new AuthorizationService(() -> null, (subject, request) -> true);
        assertEquals(AuthorizationDecision.UNAVAILABLE, service.authorize(REQUEST));
    }

    @Test
    void aPreviousPermissionIsNotReusedAfterPolicyChanges() {
        AtomicBoolean allowed = new AtomicBoolean(true);
        AuthorizationService service = new AuthorizationService(() -> Optional.of(SUBJECT),
                (subject, request) -> allowed.get());
        assertTrue(service.authorize(REQUEST).allowsAccess());
        allowed.set(false);
        assertFalse(service.authorize(REQUEST).allowsAccess());
    }

    @Test
    void missingScopeIsNotAnAutomaticGlobalGrant() {
        AuthorizationService service = new AuthorizationService(() -> Optional.of(SUBJECT),
                (subject, request) -> request.scope().equals(REQUEST.scope()));
        assertEquals(AuthorizationDecision.DENIED, service.authorize(
                new AuthorizationRequest(REQUEST.action(), REQUEST.resource(), Optional.empty())));
    }

    @Test
    void invalidWiringAndMissingRequestAreRejected() {
        assertThrows(NullPointerException.class, () -> new AuthorizationService(null, (subject, request) -> true));
        assertThrows(NullPointerException.class, () -> new AuthorizationService(Optional::empty, null));
        AuthorizationService service = new AuthorizationService(Optional::empty, (subject, request) -> true);
        assertThrows(NullPointerException.class, () -> service.authorize(null));
    }

    @ParameterizedTest
    @EnumSource(AuthorizationDecision.class)
    void nonAllowedDecisionsCannotBeTreatedAsPermission(AuthorizationDecision decision) {
        assertEquals(decision == AuthorizationDecision.ALLOWED, decision.allowsAccess());
    }
}
