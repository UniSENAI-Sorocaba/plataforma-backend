package br.senai.plataformadigital.auth.api.authorization;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Harness HTTP isolado: não é controller da aplicação nem define política institucional. */
class AuthorizationHttpHarnessTests {
    private static final String ROUTE = "/__test/authorization/scopes/{scope}/resources/{resource}";
    private static final AuthenticatedSubject SUBJECT = new AuthenticatedSubject("synthetic-server-subject");
    private static final AuthorizationPolicy SYNTHETIC_POLICY = (subject, request) ->
            subject.equals(SUBJECT)
                    && request.action().equals("synthetic:read")
                    && request.resource().equals("synthetic-resource-a")
                    && request.scope().equals(Optional.of("synthetic-scope-a"));

    @Test
    void explicitPermissionInMatchingScopeReturns200InHarness() throws Exception {
        mvc(() -> Optional.of(SUBJECT)).perform(get(ROUTE, "synthetic-scope-a", "synthetic-resource-a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("allowed"));
    }

    @Test
    void authenticatedSubjectCannotAccessAnotherScope() throws Exception {
        mvc(() -> Optional.of(SUBJECT)).perform(get(ROUTE, "synthetic-scope-b", "synthetic-resource-a"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value("access_denied"));
    }

    @Test
    void authenticatedSubjectCannotSwapResourceIdentifier() throws Exception {
        mvc(() -> Optional.of(SUBJECT)).perform(get(ROUTE, "synthetic-scope-a", "synthetic-resource-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    void arbitraryClientTokenAndRolesDoNotCreateAnAuthenticatedSubject() throws Exception {
        mvc(Optional::empty).perform(get(ROUTE, "synthetic-scope-a", "synthetic-resource-a")
                        .header("Authorization", "Bearer synthetic-unvalidated-token")
                        .header("X-Roles", "ADMIN")
                        .param("subjectId", SUBJECT.id())
                        .param("allowed", "true"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value("authentication_required"));
    }

    @Test
    void clientSubjectAndPermissionFieldsCannotOverrideTrustedIdentity() throws Exception {
        mvc(() -> Optional.of(new AuthenticatedSubject("synthetic-other-subject")))
                .perform(get(ROUTE, "synthetic-scope-a", "synthetic-resource-a")
                        .header("X-Subject-Id", SUBJECT.id())
                        .header("X-Roles", "ADMIN")
                        .param("permissions", "*")
                        .param("subjectId", SUBJECT.id()))
                .andExpect(status().isForbidden());
    }

    private MockMvc mvc(AuthenticatedSubjectProvider subjects) {
        return MockMvcBuilders.standaloneSetup(new HarnessController(
                new AuthorizationService(subjects, SYNTHETIC_POLICY))).build();
    }

    @RestController
    @TestComponent
    static class HarnessController {
        private final AuthorizationService authorization;

        HarnessController(AuthorizationService authorization) {
            this.authorization = authorization;
        }

        @GetMapping(ROUTE)
        ResponseEntity<Map<String, String>> read(@PathVariable String scope, @PathVariable String resource) {
            AuthorizationDecision decision = authorization.authorize(
                    new AuthorizationRequest("synthetic:read", resource, Optional.of(scope)));
            if (decision.allowsAccess()) {
                return ResponseEntity.ok(Map.of("status", "allowed"));
            }
            if (decision == AuthorizationDecision.UNAUTHENTICATED) {
                return ResponseEntity.status(401).body(Map.of("status", "authentication_required"));
            }
            return ResponseEntity.status(403).body(Map.of("status", "access_denied"));
        }
    }
}
