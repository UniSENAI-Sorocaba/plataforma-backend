package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plataforma.dto.RubricCreateRequest;
import plataforma.dto.RubricCriterionRequest;
import plataforma.dto.RubricVersionRequest;
import plataforma.exception.RubricRuleViolationException;
import plataforma.model.Rubric;
import plataforma.model.RubricVersion;
import plataforma.repository.RubricRepository;
import plataforma.repository.RubricVersionRepository;
import plataforma.rubric.RubricFormula;

class RubricServiceTest {

    private RubricRepository rubricRepository;
    private RubricVersionRepository versionRepository;
    private AuditEventService auditEventService;
    private RubricService service;

    @BeforeEach
    void setUp() {
        rubricRepository = mock(RubricRepository.class);
        versionRepository = mock(RubricVersionRepository.class);
        auditEventService = mock(AuditEventService.class);
        var subjects = mock(AuthenticatedSubjectProvider.class);
        when(subjects.currentSubject()).thenReturn(Optional.empty());
        service = new RubricService(rubricRepository, versionRepository, auditEventService, subjects);
    }

    @Test
    void rejeitaVersaoComPesosQueNaoSomamUm() {
        var request = createRequest(List.of(
                criterion("technical", "0.60"), criterion("artifact", "0.30")));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RubricRuleViolationException.class)
                .hasMessageContaining("soma dos pesos");
        verify(rubricRepository, never()).saveAndFlush(any(Rubric.class));
    }

    @Test
    void criaPrimeiraVersaoERegistraAuditoria() {
        when(rubricRepository.existsByCode("PI-2026")).thenReturn(false);
        when(rubricRepository.saveAndFlush(any(Rubric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(createRequest(List.of(
                criterion("technical", "0.50"), criterion("artifact", "0.50"))));

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.criteria()).extracting(item -> item.code())
                .containsExactly("technical", "artifact");
        verify(auditEventService).record(any());
    }

    @Test
    void proximaVersaoRecebeNumeroNovoSemAlterarAAnterior() {
        var rubricId = UUID.randomUUID();
        var rubric = new Rubric("PI-2026", "Projeto Integrador", null);
        var previous = new RubricVersion(1, BigDecimal.ZERO, BigDecimal.TEN, 2,
                RoundingMode.HALF_UP, RubricFormula.WEIGHTED_AVERAGE, null);
        rubric.addVersion(previous);
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(rubric));
        when(versionRepository.findTopByRubricIdOrderByVersionNumberDesc(rubricId))
                .thenReturn(Optional.of(previous));
        when(rubricRepository.saveAndFlush(any(Rubric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createVersion(rubricId, versionRequest(List.of(
                criterion("technical", "1.00"))));

        assertThat(response.version()).isEqualTo(2);
        assertThat(rubric.getVersions()).hasSize(2);
        assertThat(rubric.getVersions().getFirst().getVersionNumber()).isEqualTo(1);
    }

    private static RubricCreateRequest createRequest(List<RubricCriterionRequest> criteria) {
        return new RubricCreateRequest("PI-2026", "Projeto Integrador", versionRequest(criteria));
    }

    private static RubricVersionRequest versionRequest(List<RubricCriterionRequest> criteria) {
        return new RubricVersionRequest(BigDecimal.ZERO, BigDecimal.TEN, 2,
                RoundingMode.HALF_UP, RubricFormula.WEIGHTED_AVERAGE, criteria);
    }

    private static RubricCriterionRequest criterion(String code, String weight) {
        return new RubricCriterionRequest(code, code, new BigDecimal(weight),
                BigDecimal.ZERO, BigDecimal.TEN, 0);
    }
}
