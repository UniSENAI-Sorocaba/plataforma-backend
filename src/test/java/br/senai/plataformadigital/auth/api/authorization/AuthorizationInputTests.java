package br.senai.plataformadigital.auth.api.authorization;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationInputTests {
    @Test
    void nullInputsAreRejectedBeforeAnyPolicyEvaluation() {
        assertThrows(NullPointerException.class, () -> new AuthenticatedSubject(null));
        assertThrows(NullPointerException.class, () -> new AuthorizationRequest(null, "r", Optional.empty()));
        assertThrows(NullPointerException.class, () -> new AuthorizationRequest("a", null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new AuthorizationRequest("a", "r", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void blankInputsAreRejected(String value) {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedSubject(value));
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationRequest(value, "r", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationRequest("a", value, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AuthorizationRequest("a", "r", Optional.of(value)));
    }

    @Test
    void opaqueIdentifiersArePreservedWithoutInventingNormalizationRules() {
        AuthorizationRequest request = new AuthorizationRequest("Synthetic:Read", "Resource-A", Optional.of("Scope-A"));
        assertEquals("Synthetic:Read", request.action());
        assertEquals("Resource-A", request.resource());
        assertEquals(Optional.of("Scope-A"), request.scope());
    }
}
