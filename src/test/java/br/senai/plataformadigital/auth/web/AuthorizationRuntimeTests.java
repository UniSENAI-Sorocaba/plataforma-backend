package br.senai.plataformadigital.auth.web;

import br.senai.plataformadigital.auth.api.authorization.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthorizationRuntimeTests.FixtureController.class, AuthorizationRuntimeTests.Fixtures.class})
class AuthorizationRuntimeTests {
    @Autowired MockMvc mvc;
    @Autowired AtomicBoolean sourceAvailable;
    @Autowired AtomicBoolean auditAvailable;
    @Autowired List<AuthorizationAudit.Event> events;
    @Autowired MeterRegistry metrics;

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean AtomicBoolean sourceAvailable() { return new AtomicBoolean(true); }
        @Bean AtomicBoolean auditAvailable() { return new AtomicBoolean(true); }
        @Bean List<AuthorizationAudit.Event> authorizationEvents() { return new CopyOnWriteArrayList<>(); }
        @Bean AuthorizationPolicy fixturePolicy(AtomicBoolean sourceAvailable) {
            return (subject, request) -> {
                if (!sourceAvailable.get()) throw new IllegalStateException("synthetic-private-source-error");
                return subject.id().equals("synthetic-reader") && request.action().equals("synthetic:read")
                        && request.resource().equals("record-a") && request.scope().orElse("").equals("scope-a");
            };
        }
        @Bean AuthorizationAudit fixtureAudit(AtomicBoolean auditAvailable, List<AuthorizationAudit.Event> events) {
            return event -> {
                if (!auditAvailable.get()) throw new IllegalStateException("synthetic-private-audit-error");
                events.add(event);
            };
        }
    }

    @RestController @TestComponent
    static class FixtureController {
        @GetMapping("/__authorization/public") String publicRoute() { return "public"; }
        @GetMapping("/__authorization/scopes/{scope}/records/{id}") String read() { return "allowed"; }
        @PostMapping("/__authorization/records") String create() { return "created"; }
    }

    @BeforeEach void reset() { sourceAvailable.set(true); auditAvailable.set(true); events.clear(); }

    @Test void publicAndFrameworkEndpointsRemainAvailable() throws Exception {
        mvc.perform(get("/__authorization/public")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        assertTrue(events.isEmpty());
    }

    @Test void runtimeUsesServerPrincipalAndIgnoresForgedRolesAndTokens() throws Exception {
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-a")
                        .header("Authorization", "Bearer synthetic-unvalidated-token").header("X-Roles", "ADMIN")
                        .param("subject", "synthetic-reader"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-a").principal(() -> "synthetic-reader"))
                .andExpect(status().isOk()).andExpect(content().string("allowed"));
        mvc.perform(post("/__authorization/records").principal(() -> "synthetic-reader"))
                .andExpect(status().isForbidden());
    }

    @Test void routeScopeAndResourceAreEnforcedWithoutTrustingQueryOverrides() throws Exception {
        for (String path : List.of("/__authorization/scopes/scope-b/records/record-a", "/__authorization/scopes/scope-a/records/record-b")) {
            mvc.perform(get(path).principal(() -> "synthetic-reader").param("scope", "scope-a").param("resource", "record-a"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(head("/__authorization/scopes/scope-a/records/record-a").principal(() -> "synthetic-reader"))
                .andExpect(status().isOk());
    }

    @Test void sourceAndAuditFailuresPreventControllerExecutionWithoutLeakingMessages() throws Exception {
        sourceAvailable.set(false);
        String body = mvc.perform(get("/__authorization/scopes/scope-a/records/record-a").principal(() -> "synthetic-reader"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value("Autorização indisponível"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("private"));
        sourceAvailable.set(true);
        auditAvailable.set(false);
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-a").principal(() -> "synthetic-reader"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test void sanitizedAuditAndBoundedMetricsRecordDecisionsWithoutRequestPayloads() throws Exception {
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-b").principal(() -> "synthetic-reader")
                        .header("X-Correlation-Id", "safe-correlation").header("Authorization", "Bearer private-value"))
                .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.correlationId").value("safe-correlation"));
        assertEquals(1, events.size());
        assertEquals("/__authorization/scopes/{scope}/records/{id}", events.getFirst().route());
        assertEquals(AuthorizationDecision.DENIED, events.getFirst().decision());
        assertEquals("synthetic-reader", events.getFirst().actor());
        assertFalse(events.toString().contains("private-value"));
        assertTrue(metrics.get("authorization.decisions").tag("outcome", "DENIED").counter().count() >= 1);
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-a").header("X-Correlation-Id", "invalid private value"))
                .andExpect(status().isUnauthorized());
        assertNotEquals("invalid private value", events.getLast().correlationId());
    }

    @Test void existingServerCorrelationIsReusedAcrossMiddlewareAndAudit() throws Exception {
        mvc.perform(get("/__authorization/scopes/scope-a/records/record-a").principal(() -> "synthetic-reader")
                        .requestAttr("request.correlationId", "server-correlation").header("X-Correlation-Id", "client-value"))
                .andExpect(status().isOk()).andExpect(header().string("X-Correlation-Id", "server-correlation"));
        assertEquals("server-correlation", events.getFirst().correlationId());
    }
}
