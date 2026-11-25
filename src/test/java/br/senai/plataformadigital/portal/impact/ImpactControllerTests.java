package br.senai.plataformadigital.portal.impact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ImpactControllerTests {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private final AtomicReference<String> actor = new AtomicReference<>("author");
    private ImpactController controller;

    @BeforeEach
    void setUp() {
        var access = new ImpactAccess() {
            @Override public Optional<String> currentActor() { return Optional.of(actor.get()); }
            @Override public boolean permits(String subject, Impact.Action action, UUID id) { return true; }
        };
        var service = new ImpactService(new InMemoryImpactRepository(), access,
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC), () -> ID);
        controller = new ImpactController(service);
    }

    @Test
    void mapsLifecycleCommandsAndReadModelsToHttpResponses() {
        var request = new MockHttpServletRequest();
        request.setAttribute(plataforma.config.CorrelationIdFilter.ATTRIBUTE, "coverage-test");
        var draft = new ImpactController.DraftInput("Title", "Summary",
                new ImpactController.ProvenanceInput("source", "owner", LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2026-06-30")));

        var created = controller.create(draft, request);
        assertEquals(201, created.getStatusCode().value());
        assertEquals("\"1\"", created.getHeaders().getETag());
        assertEquals("/api/v1/impacts/" + ID + "/versions", created.getHeaders().getLocation().getPath());
        assertEquals(1, created.getBody().latestVersion());

        actor.set("approver");
        var approved = controller.approve(ID, 1, "\"1\"", request);
        assertEquals("\"2\"", approved.getHeaders().getETag());
        actor.set("publisher");
        var published = controller.publish(ID, "\"2\"", new ImpactController.PublicationInput(1), request);
        assertEquals(1L, published.getBody().publishedVersion());

        var listed = controller.versions(ID, 0, 1).getBody();
        assertEquals(1, listed.totalElements());
        assertTrue(listed.content().getFirst().approved());
        assertFalse(controller.publicImpact(ID).title().isBlank());
        assertEquals("source", controller.provenance(ID, 1).sourceReference());
        assertEquals("CREATE", controller.history(ID, 0, 10).content().getFirst().action());

        actor.set("author");
        var withdrawn = controller.withdraw(ID, "\"3\"", new ImpactController.WithdrawalInput("No longer current"), request);
        assertEquals("\"4\"", withdrawn.getHeaders().getETag());
        assertEquals("No longer current", controller.history(ID, 0, 10).content().getLast().withdrawalReason());
    }

    @Test
    void rejectsMalformedRevisionAndInvalidPageBounds() {
        var request = new MockHttpServletRequest();
        controller.create(new ImpactController.DraftInput("Title", "Summary",
                new ImpactController.ProvenanceInput("source", "owner", LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2026-06-30"))), request);
        var invalidRevision = assertThrows(ImpactFailure.class,
                () -> controller.approve(ID, 1, "1", request));
        assertEquals(400, invalidRevision.status());
        assertEquals(400, assertThrows(ImpactFailure.class, () -> controller.versions(ID, -1, 10)).status());
        assertEquals(400, assertThrows(ImpactFailure.class, () -> controller.history(ID, 0, 101)).status());
    }

    @Test
    void strictInputsRejectUnknownJsonProperties() {
        var input = new ImpactController.DraftInput("Title", "Summary",
                new ImpactController.ProvenanceInput("source", "owner", LocalDate.now(), LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> input.rejectUnknownField("unexpected", true));
    }
}
