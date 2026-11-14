package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plataforma.dto.EvaluationCriterionScoreRequest;
import plataforma.dto.EvaluationRequest;
import plataforma.exception.EvaluationAccessDeniedException;
import plataforma.mapper.EvaluationMapper;
import plataforma.model.BoardStatus;
import plataforma.model.ExaminationBoard;
import plataforma.model.Role;
import plataforma.model.Rubric;
import plataforma.model.RubricCriterion;
import plataforma.model.RubricVersion;
import plataforma.model.UserAccount;
import plataforma.repository.EvaluationRepository;
import plataforma.repository.ExaminationBoardRepository;
import plataforma.repository.RubricVersionRepository;
import plataforma.rubric.RubricCalculator;

class EvaluationServiceTest {

    private final UUID evaluatorId = UUID.randomUUID();
    private EvaluationRepository repository;
    private ExaminationBoard board;
    private UserAccount evaluator;
    private RubricVersion version;
    private EvaluationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EvaluationRepository.class);
        var boardService = mock(ExaminationBoardService.class);
        var userService = mock(UserService.class);
        var versionRepository = mock(RubricVersionRepository.class);
        var boardRepository = mock(ExaminationBoardRepository.class);
        var subjects = mock(AuthenticatedSubjectProvider.class);
        var audit = mock(AuditEventService.class);

        board = mock(ExaminationBoard.class);
        when(board.getStatus()).thenReturn(BoardStatus.IN_EVALUATION);
        when(board.hasEvaluator(any(UserAccount.class))).thenReturn(true);
        when(board.getEvaluators()).thenReturn(java.util.Set.of());
        evaluator = mock(UserAccount.class);
        when(evaluator.getId()).thenReturn(evaluatorId);
        when(evaluator.isActive()).thenReturn(true);
        when(evaluator.getRole()).thenReturn(Role.PROFESSOR);
        when(boardService.findEntity(any(UUID.class))).thenReturn(board);
        when(userService.findEntity(any(UUID.class))).thenReturn(evaluator);
        when(subjects.currentSubject()).thenReturn(Optional.of(new AuthenticatedSubject(evaluatorId.toString())));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var rubric = new Rubric("PI-2026", "Projeto Integrador", null);
        version = new RubricVersion(1, BigDecimal.ZERO, BigDecimal.TEN, 2,
                RoundingMode.HALF_UP, plataforma.rubric.RubricFormula.WEIGHTED_AVERAGE, null);
        version.addCriterion(new RubricCriterion("technical", "Técnico", new BigDecimal("0.25"),
                BigDecimal.ZERO, BigDecimal.TEN, 0));
        version.addCriterion(new RubricCriterion("artifact", "Artefato", new BigDecimal("0.75"),
                BigDecimal.ZERO, BigDecimal.TEN, 1));
        rubric.addVersion(version);
        when(versionRepository.findById(any(UUID.class))).thenReturn(Optional.of(version));

        service = new EvaluationService(repository, new EvaluationMapper(), boardService, userService,
                versionRepository, boardRepository, new RubricCalculator(), subjects, audit);
    }

    @Test
    void calculaNotaPelaVersaoPersistidaDaRubrica() {
        var response = service.create(new EvaluationRequest(
                UUID.randomUUID(), evaluatorId, null, null, null, "ok", UUID.randomUUID(),
                List.of(new EvaluationCriterionScoreRequest("technical", new BigDecimal("8")),
                        new EvaluationCriterionScoreRequest("artifact", new BigDecimal("10")))));

        assertThat(response.finalGrade()).isEqualByComparingTo("9.50");
        assertThat(response.rubricVersionId()).isNull();
        assertThat(response.criterionScores()).extracting(item -> item.criterionCode())
                .containsExactly("technical", "artifact");
    }

    @Test
    void bloqueiaAvaliadorDiferenteDaIdentidadeAutenticada() {
        var other = UUID.randomUUID();
        var subjects = mock(AuthenticatedSubjectProvider.class);
        when(subjects.currentSubject()).thenReturn(Optional.of(new AuthenticatedSubject(other.toString())));
        var boardService = mock(ExaminationBoardService.class);
        var userService = mock(UserService.class);
        when(boardService.findEntity(any(UUID.class))).thenReturn(board);
        when(userService.findEntity(any(UUID.class))).thenReturn(evaluator);
        service = new EvaluationService(repository, new EvaluationMapper(), boardService, userService,
                mock(RubricVersionRepository.class), mock(ExaminationBoardRepository.class),
                new RubricCalculator(), subjects, mock(AuditEventService.class));

        assertThatThrownBy(() -> service.create(new EvaluationRequest(
                UUID.randomUUID(), evaluatorId, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null)))
                .isInstanceOf(EvaluationAccessDeniedException.class);
    }

    @Test
    void bloqueiaEdicaoDaAvaliacaoDeOutroAvaliador() {
        var existing = new plataforma.model.Evaluation();
        existing.setBoard(board);
        existing.setEvaluator(evaluator);
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(existing));

        var other = UUID.randomUUID();
        var subjects = mock(AuthenticatedSubjectProvider.class);
        when(subjects.currentSubject()).thenReturn(Optional.of(new AuthenticatedSubject(other.toString())));
        service = new EvaluationService(repository, new EvaluationMapper(), mock(ExaminationBoardService.class),
                mock(UserService.class), mock(RubricVersionRepository.class), mock(ExaminationBoardRepository.class),
                new RubricCalculator(), subjects, mock(AuditEventService.class));

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), new EvaluationRequest(
                UUID.randomUUID(), evaluatorId, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null)))
                .isInstanceOf(EvaluationAccessDeniedException.class);
    }
}
