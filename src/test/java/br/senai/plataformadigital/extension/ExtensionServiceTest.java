package br.senai.plataformadigital.extension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ExtensionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID OPPORTUNITY = UUID.randomUUID();
    private static final UUID APPLICATION = UUID.randomUUID();

    @Mock ExtensionRepository repository;
    private ExtensionService service;

    @BeforeEach
    void setUp() { service = new ExtensionService(repository, Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test
    void rejectsApplicationBeforeWindow() {
        when(repository.isActiveStudent(STUDENT)).thenReturn(true);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(opportunity(NOW.plusSeconds(1), NOW.plusSeconds(60)));

        assertThatThrownBy(() -> service.apply(OPPORTUNITY, STUDENT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("janela");
        verify(repository, never()).insertApplication(any(), any());
    }

    @Test
    void acceptsApplicationAtInclusiveStart() {
        when(repository.isActiveStudent(STUDENT)).thenReturn(true);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(opportunity(NOW, NOW.plusSeconds(60)));
        ApplicationResponse expected = new ApplicationResponse(APPLICATION, OPPORTUNITY, STUDENT,
                ApplicationStatus.SUBMITTED, NOW, null);
        when(repository.insertApplication(OPPORTUNITY, STUDENT)).thenReturn(expected);

        org.assertj.core.api.Assertions.assertThat(service.apply(OPPORTUNITY, STUDENT)).isEqualTo(expected);
    }

    @Test
    void rejectsApplicationAtExclusiveEnd() {
        when(repository.isActiveStudent(STUDENT)).thenReturn(true);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(opportunity(NOW.minusSeconds(60), NOW));

        assertThatThrownBy(() -> service.apply(OPPORTUNITY, STUDENT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("janela");
        verify(repository, never()).insertApplication(any(), any());
    }

    @Test
    void mapsDuplicateApplicationToConflict() {
        when(repository.isActiveStudent(STUDENT)).thenReturn(true);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(opportunity(NOW.minusSeconds(60), NOW.plusSeconds(60)));
        when(repository.insertApplication(OPPORTUNITY, STUDENT)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.apply(OPPORTUNITY, STUDENT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui");
    }

    @Test
    void createsOnlyForCoordinatorAndRejectsInvalidWindow() {
        when(repository.hasRole(STUDENT, "COORDENACAO")).thenReturn(true);
        OpportunityRequest request = new OpportunityRequest(null, "Edital de Extensão", "Descrição suficiente para o edital.", NOW, NOW);
        assertThatThrownBy(() -> service.create(request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("janela");
        verify(repository, never()).insertOpportunity(any(), any());
    }

    @Test
    void publishesAndClosesWithSequentialTransitions() {
        when(repository.hasRole(STUDENT, "COORDENACAO")).thenReturn(true);
        OpportunityResponse published = opportunity(NOW.minusSeconds(60), NOW.plusSeconds(60));
        OpportunityResponse closed = new OpportunityResponse(OPPORTUNITY, null, published.title(), published.description(),
                OpportunityStatus.CLOSED, published.startsAt(), published.endsAt(), NOW, NOW);
        when(repository.updateStatus(eq(OPPORTUNITY), eq(OpportunityStatus.DRAFT), eq(OpportunityStatus.PUBLISHED), any())).thenReturn(1);
        when(repository.updateStatus(eq(OPPORTUNITY), eq(OpportunityStatus.PUBLISHED), eq(OpportunityStatus.CLOSED), any())).thenReturn(1);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(published, closed);

        assertThat(service.publish(OPPORTUNITY, STUDENT).status()).isEqualTo(OpportunityStatus.PUBLISHED);
        assertThat(service.close(OPPORTUNITY, STUDENT).status()).isEqualTo(OpportunityStatus.CLOSED);
    }

    @Test
    void withdrawsOwnApplicationOnlyWhileWindowIsOpen() {
        when(repository.isActiveStudent(STUDENT)).thenReturn(true);
        when(repository.findApplication(APPLICATION)).thenReturn(
                new ApplicationResponse(APPLICATION, OPPORTUNITY, STUDENT, ApplicationStatus.SUBMITTED, NOW, null),
                new ApplicationResponse(APPLICATION, OPPORTUNITY, STUDENT, ApplicationStatus.WITHDRAWN, NOW, NOW));
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(opportunity(NOW.minusSeconds(60), NOW.plusSeconds(60)));
        when(repository.withdrawApplication(eq(APPLICATION), eq(STUDENT), any())).thenReturn(1);

        assertThat(service.withdraw(APPLICATION, STUDENT).status()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void coordinatorCanListUpdateAndDeleteDraftOpportunities() {
        when(repository.hasRole(STUDENT, "COORDENACAO")).thenReturn(true);
        var request = new OpportunityRequest(null, "Edital atualizado", "Descrição suficiente para atualizar o edital.", NOW, NOW.plusSeconds(60));
        var expected = opportunity(NOW, NOW.plusSeconds(60));
        when(repository.findAllOpportunities()).thenReturn(java.util.List.of(expected));
        when(repository.updateOpportunity(OPPORTUNITY, request)).thenReturn(1);
        when(repository.findOpportunity(OPPORTUNITY)).thenReturn(expected);
        when(repository.deleteOpportunity(OPPORTUNITY)).thenReturn(1);

        assertThat(service.allOpportunities(STUDENT)).containsExactly(expected);
        assertThat(service.update(OPPORTUNITY, request, STUDENT)).isEqualTo(expected);
        service.delete(OPPORTUNITY, STUDENT);
        verify(repository).deleteOpportunity(OPPORTUNITY);
    }

    private static OpportunityResponse opportunity(Instant startsAt, Instant endsAt) {
        return new OpportunityResponse(OPPORTUNITY, null, "Edital de Extensão", "Descrição suficiente para o edital.",
                OpportunityStatus.PUBLISHED, startsAt, endsAt, NOW, NOW);
    }
}
