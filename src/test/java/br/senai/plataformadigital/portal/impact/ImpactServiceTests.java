package br.senai.plataformadigital.portal.impact;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class ImpactServiceTests {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    private static final Impact.Content CONTENT = new Impact.Content("Synthetic title", "Synthetic summary");
    private static final Impact.Provenance SOURCE = new Impact.Provenance("synthetic-private-source", "responsible",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    private static final ImpactAccess ALLOW = new ImpactAccess() {
        public Optional<String> currentActor() { return Optional.of("synthetic-actor"); }
        public boolean permits(String actor, Impact.Action action, UUID id) { return true; }
    };

    private ImpactService service(ImpactRepository repository, ImpactAccess access) {
        return new ImpactService(repository, access, CLOCK, () -> ID);
    }

    @Test
    void stateAndAuditHistoryAreCommittedTogetherAndCannotBeMutatedByConsumers() {
        var repository = new InMemoryImpactRepository();
        ImpactService service = service(repository, ALLOW);
        Impact draft = service.create(CONTENT, SOURCE, "correlation");
        service(repository, access(() -> Optional.of("synthetic-approver"), true)).approve(ID, 1, 1, "correlation");
        service.publish(ID, 1, 2, "correlation");
        Impact withdrawn = service.withdraw(ID, 3, "Synthetic reason", "correlation");
        assertEquals(4, withdrawn.revision());
        assertTrue(withdrawn.versions().get(1L).withdrawn());
        assertEquals("Synthetic reason", withdrawn.history().getLast().withdrawalReason());
        assertEquals(List.of(Impact.Action.CREATE, Impact.Action.APPROVE, Impact.Action.PUBLISH, Impact.Action.WITHDRAW),
                service.history(ID).stream().map(Impact.Event::action).toList());
        for (Impact.Event event : withdrawn.history()) {
            assertEquals(event.action() == Impact.Action.APPROVE ? "synthetic-approver" : "synthetic-actor", event.actor());
            assertEquals(CLOCK.instant(), event.at());
            assertEquals("correlation", event.correlationId());
            assertEquals(ID, event.impactId());
            assertEquals("SUCCESS", event.result());
        }
        assertEquals(SOURCE, service.provenance(ID, 1).provenance());
        assertEquals(1, draft.history().size());
        assertThrows(UnsupportedOperationException.class, () -> withdrawn.history().clear());
        assertThrows(UnsupportedOperationException.class, () -> withdrawn.versions().clear());
        assertEquals(404, assertThrows(ImpactFailure.class, () -> service.publicVersion(ID)).status());
        assertEquals(409, assertThrows(ImpactFailure.class, () -> service.create(CONTENT, SOURCE, "c")).status());
    }

    @Test
    void concurrentCommandsWithTheSameRevisionHaveExactlyOneWinnerAndOneEvent() throws Exception {
        var delegate = new InMemoryImpactRepository();
        service(delegate, ALLOW).create(CONTENT, SOURCE, "create");
        var barrier = new CyclicBarrier(2);
        ImpactRepository racing = new ImpactRepository() {
            public Optional<Impact> find(UUID id) { return delegate.find(id); }
            public boolean create(Impact impact) { return delegate.create(impact); }
            public boolean replace(UUID id, long expected, Impact impact) {
                try { barrier.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
                return delegate.replace(id, expected, impact);
            }
        };
        ImpactService service = service(racing, ALLOW);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var command = (java.util.concurrent.Callable<Integer>) () -> {
                try { service.revise(ID, 1, CONTENT, SOURCE, "race"); return 200; }
                catch (ImpactFailure failure) { return failure.status(); }
            };
            var first = executor.submit(command);
            var second = executor.submit(command);
            assertEquals(List.of(200, 412), Stream.of(first.get(), second.get()).sorted().toList());
        }
        assertEquals(2, delegate.find(ID).orElseThrow().history().size());
        assertEquals(2, delegate.find(ID).orElseThrow().versions().size());
        assertFalse(delegate.replace(UUID.randomUUID(), 1, delegate.find(ID).orElseThrow()));
    }

    @Test
    void absentIdentityAndPermissionsFailBeforeAccessingStorage() {
        ImpactRepository unused = repository(() -> { throw new AssertionError("Não acessar armazenamento sem autorização"); });
        for (Optional<String> actor : List.of(Optional.<String>empty(), Optional.of(" "))) {
            ImpactAccess access = access(() -> actor, true);
            assertEquals(401, assertThrows(ImpactFailure.class, () -> service(unused, access).read(ID)).status());
        }
        assertEquals(403, assertThrows(ImpactFailure.class,
                () -> service(unused, access(() -> Optional.of("actor"), false)).read(ID)).status());
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(unused, access(() -> null, true)).read(ID)).status());
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(unused, access(() -> { throw new IllegalStateException("private"); }, true)).read(ID)).status());
        ImpactAccess brokenPermissions = new ImpactAccess() {
            public Optional<String> currentActor() { return Optional.of("actor"); }
            public boolean permits(String actor, Impact.Action action, UUID id) { throw new IllegalStateException("private"); }
        };
        assertEquals(503, assertThrows(ImpactFailure.class, () -> service(unused, brokenPermissions).read(ID)).status());
    }

    @Test
    void repositoryFailuresNeverExposePrivateErrorsOrStalePublicData() {
        ImpactRepository failing = repository(() -> { throw new IllegalStateException("private-database-password"); });
        ImpactService service = service(failing, ALLOW);
        var failure = assertThrows(ImpactFailure.class, () -> service.publicVersion(ID));
        assertEquals(503, failure.status());
        assertFalse(failure.getMessage().contains("password"));
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service.create(CONTENT, SOURCE, "c")).status());
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(repository(() -> null), ALLOW).publicVersion(ID)).status());
    }

    @Test
    void corruptedPublicationPointersOrUnapprovedRowsNeverBecomePublic() {
        Impact.Version version = new Impact.Version(1, CONTENT, SOURCE, "actor", CLOCK.instant(), null, false);
        Impact unapproved = new Impact(ID, 1, Map.of(1L, version), 1L, List.of());
        assertEquals(404, assertThrows(ImpactFailure.class,
                () -> service(repository(() -> Optional.of(unapproved)), ALLOW).publicVersion(ID)).status());
        Impact wrongId = new Impact(UUID.randomUUID(), 1, Map.of(1L, version), 1L, List.of());
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(repository(() -> Optional.of(wrongId)), ALLOW).publicVersion(ID)).status());
        Impact wrongVersion = new Impact(ID, 1, Map.of(2L, version), 2L, List.of());
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(repository(() -> Optional.of(wrongVersion)), ALLOW).publicVersion(ID)).status());
        Impact missingVersion = new Impact(ID, 1, Map.of(), 1L, List.of());
        assertEquals(404, assertThrows(ImpactFailure.class,
                () -> service(repository(() -> Optional.of(missingVersion)), ALLOW).publicVersion(ID)).status());
    }

    @Test
    void failedWriteLeavesPublishedVersionAndAuditUnchanged() {
        var delegate = new InMemoryImpactRepository();
        service(delegate, ALLOW).create(CONTENT, SOURCE, "c");
        ImpactRepository broken = new ImpactRepository() {
            public Optional<Impact> find(UUID id) { return delegate.find(id); }
            public boolean create(Impact impact) { throw new IllegalStateException("private"); }
            public boolean replace(UUID id, long revision, Impact impact) { throw new IllegalStateException("private"); }
        };
        assertEquals(503, assertThrows(ImpactFailure.class,
                () -> service(broken, access(() -> Optional.of("synthetic-approver"), true)).approve(ID, 1, 1, "c")).status());
        assertEquals(1, delegate.find(ID).orElseThrow().history().size());
        assertNull(delegate.find(ID).orElseThrow().versions().get(1L).approval());
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(null, SOURCE), Arguments.of(CONTENT, null),
                Arguments.of(new Impact.Content(null, "s"), SOURCE), Arguments.of(new Impact.Content(" ", "s"), SOURCE),
                Arguments.of(new Impact.Content("t", null), SOURCE), Arguments.of(new Impact.Content("t", " "), SOURCE),
                Arguments.of(CONTENT, new Impact.Provenance(null, "r", SOURCE.periodStart(), SOURCE.periodEnd())),
                Arguments.of(CONTENT, new Impact.Provenance(" ", "r", SOURCE.periodStart(), SOURCE.periodEnd())),
                Arguments.of(CONTENT, new Impact.Provenance("s", null, SOURCE.periodStart(), SOURCE.periodEnd())),
                Arguments.of(CONTENT, new Impact.Provenance("s", " ", SOURCE.periodStart(), SOURCE.periodEnd())),
                Arguments.of(CONTENT, new Impact.Provenance("s", "r", null, SOURCE.periodEnd())),
                Arguments.of(CONTENT, new Impact.Provenance("s", "r", SOURCE.periodStart(), null)),
                Arguments.of(CONTENT, new Impact.Provenance("s", "r", SOURCE.periodEnd(), SOURCE.periodStart())));
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void invalidContentAndProvenanceNeverCreateAnImpact(Impact.Content content, Impact.Provenance provenance) {
        var repository = new InMemoryImpactRepository();
        assertEquals(400, assertThrows(ImpactFailure.class,
                () -> service(repository, ALLOW).create(content, provenance, "c")).status());
        assertTrue(repository.find(ID).isEmpty());
    }

    private ImpactAccess access(java.util.function.Supplier<Optional<String>> identity, boolean allowed) {
        return new ImpactAccess() {
            public Optional<String> currentActor() { return identity.get(); }
            public boolean permits(String actor, Impact.Action action, UUID id) { return allowed; }
        };
    }

    private ImpactRepository repository(java.util.function.Supplier<Optional<Impact>> read) {
        return new ImpactRepository() {
            public Optional<Impact> find(UUID id) { return read.get(); }
            public boolean create(Impact impact) { read.get(); return false; }
            public boolean replace(UUID id, long revision, Impact impact) { read.get(); return false; }
        };
    }
}
