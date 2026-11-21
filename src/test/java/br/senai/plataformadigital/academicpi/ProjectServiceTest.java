package br.senai.plataformadigital.academicpi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectRepository repository;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(repository, Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void delegatesPublicSearchWithStablePagination() {
        var expected = new PublicProjectPage(List.of(), 1, 10, 0, 0);
        when(repository.findPublicPage("ENG", "2026.2", "ODS-9", 1, 10)).thenReturn(expected);

        var actual = service.publicProjectSearch("ENG", "2026.2", "ODS-9", 1, 10);

        org.assertj.core.api.Assertions.assertThat(actual).isSameAs(expected);
        verify(repository).findPublicPage("ENG", "2026.2", "ODS-9", 1, 10);
    }

    @Test
    void rejectsInvalidPublicPaginationBeforeQuery() {
        assertThatThrownBy(() -> service.publicProjectSearch(null, null, null, -1, 20))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.publicProjectSearch(null, null, null, 0, 0))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.publicProjectSearch(null, null, null, 0, 101))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void allowsCoordinationToExtendOrReopenOnlyIntoTheFuture() {
        UUID project = UUID.randomUUID();
        UUID period = UUID.randomUUID();
        var endsAt = Instant.parse("2026-12-01T12:00:00Z");
        var expected = new EnrollmentPeriodResponse(period, project, "2026.2",
                Instant.parse("2026-11-01T12:00:00Z"), endsAt,
                Instant.parse("2026-10-01T12:00:00Z"), Instant.parse("2026-11-21T12:00:00Z"));
        when(repository.hasRole(UUID.fromString("00000000-0000-0000-0000-000000000001"), "COORDENACAO")).thenReturn(true);
        when(repository.updatePeriodEnd(project, period, endsAt)).thenReturn(1);
        when(repository.findPeriod(project, period)).thenReturn(expected);

        var actual = service.updatePeriod(project, period,
                new EnrollmentPeriodUpdateRequest(endsAt),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        org.assertj.core.api.Assertions.assertThat(actual).isSameAs(expected);
        verify(repository).updatePeriodEnd(project, period, endsAt);
    }

    @Test
    void rejectsDeadlineAdjustmentThatIsNotFuture() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(repository.hasRole(actor, "COORDENACAO")).thenReturn(true);
        assertThatThrownBy(() -> service.updatePeriod(UUID.randomUUID(), UUID.randomUUID(),
                new EnrollmentPeriodUpdateRequest(Instant.parse("2026-11-21T12:00:00Z")), actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("futuro");
    }
}
