package br.senai.plataformadigital.portal.api.impact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicImpactQueryServiceTests {
    private static final String IMPACT = "synthetic-impact";
    private static final ObjectMapper JSON = new ObjectMapper();

    static Stream<JsonNode> publicationFixtures() throws IOException {
        try (var input = PublicImpactQueryServiceTests.class.getResourceAsStream("/impact/publication-fixtures.json")) {
            JsonNode fixtures = JSON.readTree(input);
            return StreamSupport.stream(fixtures.spliterator(), false);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("publicationFixtures")
    void onlyExplicitPublicationOfTheApprovedVersionIsReturned(JsonNode fixture) {
        ImpactVersion version = new ImpactVersion(fixture.path("impactId").asText(),
                fixture.path("versionId").asText(), fixture.path("originReference").asText());
        Optional<ImpactApproval> approval = fixture.has("approvalVersionId")
                ? Optional.of(new ImpactApproval(fixture.path("approvalImpactId").asText(),
                    fixture.path("approvalVersionId").asText())) : Optional.empty();
        ImpactPublicationSnapshot snapshot = new ImpactPublicationSnapshot(version, approval,
                fixture.path("published").asBoolean(), fixture.path("withdrawn").asBoolean());
        PublicImpactQueryService service = new PublicImpactQueryService(id -> Optional.of(snapshot));
        Optional<PublicImpactReference> result = service.findPublished(IMPACT);
        assertEquals(fixture.path("visible").asBoolean(), result.isPresent());
        result.ifPresent(reference -> {
            assertEquals(IMPACT, reference.impactId());
            assertEquals(version.versionId(), reference.versionId());
        });
    }

    @Test
    void withdrawalRemovesSelectionWithoutDestroyingVersionAndOriginReferences() {
        ImpactVersion version = new ImpactVersion(IMPACT, "v1", "synthetic-private-source");
        ImpactPublicationSnapshot original = new ImpactPublicationSnapshot(version,
                Optional.of(new ImpactApproval(IMPACT, "v1")), true, false);
        AtomicReference<ImpactPublicationSnapshot> source = new AtomicReference<>(original);
        PublicImpactQueryService service = new PublicImpactQueryService(id -> Optional.of(source.get()));
        assertTrue(service.findPublished(IMPACT).isPresent());
        source.set(original.withdraw());
        assertTrue(service.findPublished(IMPACT).isEmpty());
        assertEquals(original.version(), source.get().version());
        assertEquals(original.approval(), source.get().approval());
        assertFalse(original.withdrawn());
        assertTrue(source.get().withdrawn());
    }

    @Test
    void draftRequiresOriginAndIsNeverPublishedByDefault() {
        ImpactPublicationSnapshot draft = ImpactPublicationSnapshot.draft(
                new ImpactVersion(IMPACT, "v1", "synthetic-private-source"));
        assertFalse(draft.published());
        assertFalse(draft.withdrawn());
        assertTrue(draft.approval().isEmpty());
        assertTrue(new PublicImpactQueryService(id -> Optional.of(draft)).findPublished(IMPACT).isEmpty());
    }

    @Test
    void publicSelectionDoesNotSerializeOriginOrApproval() throws IOException {
        ImpactPublicationSnapshot snapshot = new ImpactPublicationSnapshot(
                new ImpactVersion(IMPACT, "v1", "synthetic-private-source"),
                Optional.of(new ImpactApproval(IMPACT, "v1")), true, false);
        PublicImpactReference result = new PublicImpactQueryService(id -> Optional.of(snapshot))
                .findPublished(IMPACT).orElseThrow();
        assertEquals(JSON.readTree("{\"impactId\":\"synthetic-impact\",\"versionId\":\"v1\"}"), JSON.valueToTree(result));
    }

    @Test
    void missingPublicationIsNotReturned() {
        assertTrue(new PublicImpactQueryService(id -> Optional.empty()).findPublished(IMPACT).isEmpty());
    }

    @Test
    void sourceFailureDoesNotReturnPreviouslyPublishedDataOrItsPrivateError() {
        ImpactPublicationSnapshot snapshot = new ImpactPublicationSnapshot(
                new ImpactVersion(IMPACT, "v1", "synthetic-private-source"),
                Optional.of(new ImpactApproval(IMPACT, "v1")), true, false);
        AtomicReference<ImpactPublicationSnapshot> source = new AtomicReference<>(snapshot);
        PublicImpactQueryService service = new PublicImpactQueryService(id -> {
            if (source.get() == null) throw new IllegalStateException("synthetic-private-error");
            return Optional.of(source.get());
        });
        assertTrue(service.findPublished(IMPACT).isPresent());
        source.set(null);
        var failure = assertThrows(ImpactPublicationUnavailableException.class, () -> service.findPublished(IMPACT));
        assertFalse(failure.getMessage().contains("synthetic-private-error"));
    }

    @Test
    void invalidSourceResultDoesNotProducePublicData() {
        assertThrows(ImpactPublicationUnavailableException.class,
                () -> new PublicImpactQueryService(id -> null).findPublished(IMPACT));
    }

    @Test
    void invalidWiringAndLookupArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new PublicImpactQueryService(null));
        PublicImpactQueryService service = new PublicImpactQueryService(id -> Optional.empty());
        assertThrows(NullPointerException.class, () -> service.findPublished(null));
        assertThrows(IllegalArgumentException.class, () -> service.findPublished(" "));
    }
}
