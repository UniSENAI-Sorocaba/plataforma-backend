package br.senai.plataformadigital.academicpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubricServiceTest {
    @Test
    void calculatesWeightedScoreWithDeterministicRounding() {
        var criteria = List.of(
                new RubricCriterionResponse(UUID.randomUUID(), "technical", "Técnica", new BigDecimal("0.60"), BigDecimal.ZERO, new BigDecimal("10")),
                new RubricCriterionResponse(UUID.randomUUID(), "impact", "Impacto", new BigDecimal("0.40"), BigDecimal.ZERO, new BigDecimal("10")));
        var result = RubricService.calculate(criteria, List.of(
                new EvaluationCriterionScoreRequest("technical", new BigDecimal("8.333")),
                new EvaluationCriterionScoreRequest("impact", new BigDecimal("7.777"))));
        assertEquals(new BigDecimal("8.11"), result);
    }

    @Test
    void rejectsMissingOrOutOfRangeCriteria() {
        var criteria = List.of(new RubricCriterionResponse(UUID.randomUUID(), "technical", "Técnica", BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("10")));
        assertThrows(ConflictException.class, () -> RubricService.calculate(criteria, List.of()));
        assertThrows(ConflictException.class, () -> RubricService.calculate(criteria, List.of(new EvaluationCriterionScoreRequest("technical", new BigDecimal("11")))));
    }

    @Test
    void rejectsInvalidWeightsWhenCreatingRubric() {
        var repository = mock(RubricRepository.class);
        var service = new RubricService(repository);
        var request = new RubricCreateRequest("PI-1", "PI", List.of(
                new RubricCriterionRequest("a", "A", new BigDecimal("0.70"), BigDecimal.ZERO, new BigDecimal("10"))));
        assertThrows(ConflictException.class, () -> service.create(request, UUID.randomUUID()));
        when(repository.isCoordinator(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void createsValidRubricAndRejectsDuplicateOrInvalidCriteria() {
        var repository = mock(RubricRepository.class);
        var service = new RubricService(repository);
        var actor = UUID.randomUUID();
        when(repository.isCoordinator(actor)).thenReturn(true);
        var valid = new RubricCreateRequest("PI-2", "Rubrica PI", List.of(
                new RubricCriterionRequest("a", "A", new BigDecimal("0.60"), BigDecimal.ZERO, new BigDecimal("10")),
                new RubricCriterionRequest("b", "B", new BigDecimal("0.40"), BigDecimal.ZERO, new BigDecimal("10"))));
        var expected = new RubricVersionResponse(UUID.randomUUID(), UUID.randomUUID(), "PI-2", "Rubrica PI", 1, List.of());
        when(repository.create(valid, actor)).thenReturn(expected);
        assertEquals(expected, service.create(valid, actor));

        var duplicate = new RubricCreateRequest("PI-3", "Rubrica PI", List.of(
                new RubricCriterionRequest("a", "A", new BigDecimal("0.50"), BigDecimal.ZERO, new BigDecimal("10")),
                new RubricCriterionRequest("a", "A2", new BigDecimal("0.50"), BigDecimal.ZERO, new BigDecimal("10"))));
        assertThrows(ConflictException.class, () -> service.create(duplicate, actor));
        var invalidRange = new RubricCreateRequest("PI-4", "Rubrica PI", List.of(
                new RubricCriterionRequest("a", "A", BigDecimal.ONE, new BigDecimal("8"), new BigDecimal("8"))));
        assertThrows(ConflictException.class, () -> service.create(invalidRange, actor));
    }

    @Test
    void rejectsDuplicateScoresAndUnknownCriteria() {
        var criterion = new RubricCriterionResponse(UUID.randomUUID(), "a", "A", BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("10"));
        assertThrows(ConflictException.class, () -> RubricService.calculate(List.of(criterion), List.of(
                new EvaluationCriterionScoreRequest("a", BigDecimal.ONE), new EvaluationCriterionScoreRequest("a", BigDecimal.TEN))));
        assertThrows(ConflictException.class, () -> RubricService.calculate(List.of(criterion), List.of(
                new EvaluationCriterionScoreRequest("unknown", BigDecimal.ONE))));
    }
}
