package br.senai.plataformadigital.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationRoutesTests {
    private AuthorizationRoutes.Rule protectedRule() {
        return new AuthorizationRoutes.Rule("GET", "/records/{id}", false, "read", null, "id", null);
    }

    @Test void rejectsUnclassifiedNewControllersAndAllowsOnlyExplicitClassification() throws Exception {
        var mappings = new RequestMappingHandlerMapping();
        mappings.registerMapping(RequestMappingInfo.paths("/records/{id}").methods(RequestMethod.GET).build(),
                new Controller(), Controller.class.getMethod("read"));
        assertThrows(IllegalStateException.class, () -> new AuthorizationRoutes(List.of()).verify(mappings));
        var catalog = new AuthorizationRoutes(List.of(protectedRule()));
        assertDoesNotThrow(() -> catalog.verify(mappings));
        assertEquals(protectedRule(), catalog.find("HEAD", "/records/{id}"));
        assertNull(catalog.find("POST", "/records/{id}"));
        assertEquals(1, catalog.definitions().size());
    }

    @Test void rejectsAmbiguousOrIncompletePolicyConfiguration() {
        for (AuthorizationRoutes.Rule invalid : List.of(
                new AuthorizationRoutes.Rule("", "/x", true, null, null, null, null),
                new AuthorizationRoutes.Rule("GET", "x", true, null, null, null, null),
                new AuthorizationRoutes.Rule("INVALID", "/x", true, null, null, null, null),
                new AuthorizationRoutes.Rule("GET", "/x", true, "read", null, null, null),
                new AuthorizationRoutes.Rule("GET", "/x", false, null, "fixed", null, null),
                new AuthorizationRoutes.Rule("GET", "/x", false, "read", null, null, null),
                new AuthorizationRoutes.Rule("GET", "/x", false, "read", "fixed", "id", null),
                new AuthorizationRoutes.Rule("GET", "/x", false, "read", null, "id", null),
                new AuthorizationRoutes.Rule("GET", "/x", false, "read", "fixed", null, "scope"),
                new AuthorizationRoutes.Rule("GET", "/x", false, "read", "fixed", null, ""))) {
            assertThrows(IllegalStateException.class, () -> new AuthorizationRoutes(List.of(invalid)));
        }
        assertThrows(IllegalStateException.class, () -> new AuthorizationRoutes(List.of(protectedRule(), protectedRule())));
    }

    @Test void configurationRequiresExplicitPublicFlagAndRejectsUnknownFields() {
        for (String invalid : List.of("{}", "[{\"method\":\"GET\",\"path\":\"/x\"}]",
                "[{\"method\":\"GET\",\"path\":\"/x\",\"publicAccess\":true,\"allowEveryone\":true}]")) {
            assertThrows(IllegalStateException.class, () -> AuthorizationRoutes.load(
                    new ByteArrayResource[]{new ByteArrayResource(invalid.getBytes(java.nio.charset.StandardCharsets.UTF_8))}, new ObjectMapper()));
        }
    }

    @Test void implicitAllMethodsMappingCannotBypassCoverage() throws Exception {
        var mappings = new RequestMappingHandlerMapping();
        mappings.registerMapping(RequestMappingInfo.paths("/records/{id}").build(), new Controller(), Controller.class.getMethod("read"));
        assertThrows(IllegalStateException.class, () -> new AuthorizationRoutes(List.of(protectedRule())).verify(mappings));
    }

    public static class Controller { public String read() { return "record"; } }
}
