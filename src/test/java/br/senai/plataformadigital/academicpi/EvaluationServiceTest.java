package br.senai.plataformadigital.academicpi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID PERIOD = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();
    private static final UUID EVALUATOR = UUID.randomUUID();
    private static final UUID COORDINATOR = UUID.randomUUID();
    private static final UUID EVALUATION = UUID.randomUUID();

    @Mock EvaluationRepository repository;
    private EvaluationService service;

    @BeforeEach
    void setUp() { service = new EvaluationService(repository); }

    @Test
    void createsEvaluationAndAssignmentsForCoordinatorWithValidLinks() {
        EvaluationRequest request = request(List.of(EVALUATOR));
        EvaluationResponse response = response();
        when(repository.isCoordinator(COORDINATOR)).thenReturn(true);
        when(repository.isActiveTarget(PROJECT, PERIOD, TARGET)).thenReturn(true);
        when(repository.isEligibleEvaluator(PROJECT, PERIOD, EVALUATOR)).thenReturn(true);
        when(repository.insert(PROJECT, PERIOD, request, COORDINATOR)).thenReturn(EVALUATION);
        when(repository.findAll(PROJECT, PERIOD)).thenReturn(List.of(response));

        service.create(PROJECT, PERIOD, request, COORDINATOR);

        verify(repository).assign(EVALUATION, EVALUATOR, COORDINATOR);
    }

    @Test
    void rejectsUnauthorizedAssignmentAndInvalidLinks() {
        EvaluationRequest request = request(List.of(EVALUATOR));
        when(repository.isCoordinator(COORDINATOR)).thenReturn(false);
        assertThatThrownBy(() -> service.create(PROJECT, PERIOD, request, COORDINATOR))
                .isInstanceOf(ConflictException.class).hasMessageContaining("coordenação");

        when(repository.isCoordinator(COORDINATOR)).thenReturn(true);
        when(repository.isActiveTarget(PROJECT, PERIOD, TARGET)).thenReturn(false);
        assertThatThrownBy(() -> service.create(PROJECT, PERIOD, request, COORDINATOR))
                .isInstanceOf(ConflictException.class).hasMessageContaining("vínculo ativo");

        verify(repository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void rejectsDuplicateAndUnlinkedEvaluators() {
        EvaluationRequest duplicate = request(List.of(EVALUATOR, EVALUATOR));
        when(repository.isCoordinator(COORDINATOR)).thenReturn(true);
        assertThatThrownBy(() -> service.create(PROJECT, PERIOD, duplicate, COORDINATOR))
                .isInstanceOf(ConflictException.class).hasMessageContaining("duplicados");

        EvaluationRequest unlinked = request(List.of(EVALUATOR));
        when(repository.isActiveTarget(PROJECT, PERIOD, TARGET)).thenReturn(true);
        when(repository.isEligibleEvaluator(PROJECT, PERIOD, EVALUATOR)).thenReturn(false);
        assertThatThrownBy(() -> service.create(PROJECT, PERIOD, unlinked, COORDINATOR))
                .isInstanceOf(ConflictException.class).hasMessageContaining("sem vínculo");
    }

    @Test
    void rejectsSelfEvaluation() {
        EvaluationRequest request = request(List.of(TARGET));
        when(repository.isCoordinator(COORDINATOR)).thenReturn(true);
        when(repository.isActiveTarget(PROJECT, PERIOD, TARGET)).thenReturn(true);
        assertThatThrownBy(() -> service.create(PROJECT, PERIOD, request, COORDINATOR))
                .isInstanceOf(ConflictException.class).hasMessageContaining("próprio");
    }

    private static EvaluationRequest request(List<UUID> evaluators) {
        return new EvaluationRequest(TARGET, Instant.parse("2026-12-01T12:00:00Z"), evaluators);
    }

    private static EvaluationResponse response() {
        Instant date = Instant.parse("2026-12-01T12:00:00Z");
        return new EvaluationResponse(EVALUATION, PROJECT, PERIOD, TARGET, date, COORDINATOR, date, date, List.of(EVALUATOR));
    }
}
