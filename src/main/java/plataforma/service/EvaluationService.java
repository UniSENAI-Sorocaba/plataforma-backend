package plataforma.service;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.EvaluationRequest;
import plataforma.dto.EvaluationResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.exception.EvaluationAccessDeniedException;
import plataforma.exception.RubricRuleViolationException;
import plataforma.mapper.EvaluationMapper;
import plataforma.model.AuditOperation;
import plataforma.model.EvaluationCriterionScore;
import plataforma.model.Evaluation;
import plataforma.model.ExaminationBoard;
import plataforma.repository.EvaluationRepository;
import plataforma.repository.ExaminationBoardRepository;
import plataforma.repository.RubricVersionRepository;
import plataforma.rubric.RubricCalculator;

@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository repository;
    private final EvaluationMapper mapper;
    private final ExaminationBoardService boardService;
    private final UserService userService;
    private final RubricVersionRepository rubricVersionRepository;
    private final ExaminationBoardRepository boardRepository;
    private final RubricCalculator calculator;
    private final AuthenticatedSubjectProvider subjects;
    private final AuditEventService auditEventService;

    public EvaluationService(EvaluationRepository repository, EvaluationMapper mapper,
            ExaminationBoardService boardService, UserService userService,
            RubricVersionRepository rubricVersionRepository, ExaminationBoardRepository boardRepository,
            RubricCalculator calculator, AuthenticatedSubjectProvider subjects,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.mapper = mapper;
        this.boardService = boardService;
        this.userService = userService;
        this.rubricVersionRepository = rubricVersionRepository;
        this.boardRepository = boardRepository;
        this.calculator = calculator;
        this.subjects = subjects;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public EvaluationResponse create(EvaluationRequest request) {
        var board = boardService.findEntity(request.boardId());
        var evaluator = userService.findEntity(request.evaluatorId());
        authorize(board, evaluator);
        var evaluation = new Evaluation();
        apply(request, evaluation, board, evaluator);
        var saved = repository.saveAndFlush(evaluation);
        consolidate(board);
        audit(AuditOperation.INSERT, saved.getId(), null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public PageResponse<EvaluationResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public EvaluationResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public EvaluationResponse update(UUID id, EvaluationRequest request) {
        var evaluation = findEntity(id);
        var before = snapshot(evaluation);
        var board = evaluation.getBoard();
        var evaluator = evaluation.getEvaluator();
        authorize(board, evaluator);
        if (!request.boardId().equals(board.getId()) || !request.evaluatorId().equals(evaluator.getId())) {
            throw new EvaluationAccessDeniedException();
        }
        apply(request, evaluation, board, evaluator);
        var saved = repository.saveAndFlush(evaluation);
        consolidate(board);
        audit(AuditOperation.UPDATE, saved.getId(), before, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        var evaluation = findEntity(id);
        authorize(evaluation.getBoard(), evaluation.getEvaluator());
        var before = snapshot(evaluation);
        repository.delete(evaluation);
        audit(AuditOperation.DELETE, id, before, null);
    }

    public Evaluation findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada: " + id));
    }

    private void apply(EvaluationRequest request, Evaluation evaluation, ExaminationBoard board,
            plataforma.model.UserAccount evaluator) {
        mapper.copyToEntity(request, evaluation);
        evaluation.setBoard(board);
        evaluation.setEvaluator(evaluator);
        if (request.rubricVersionId() == null) {
            applyLegacy(request, evaluation);
            return;
        }
        var version = rubricVersionRepository.findById(request.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Versão de rubrica não encontrada: "
                        + request.rubricVersionId()));
        var byCode = request.criterionScores().stream().collect(java.util.stream.Collectors.toMap(
                plataforma.dto.EvaluationCriterionScoreRequest::criterionCode,
                plataforma.dto.EvaluationCriterionScoreRequest::score,
                (first, ignored) -> { throw new RubricRuleViolationException("Critério repetido"); },
                LinkedHashMap::new));
        BigDecimal finalGrade;
        try {
            finalGrade = calculator.calculate(version.calculationPolicy(), byCode);
        } catch (IllegalArgumentException invalid) {
            throw new RubricRuleViolationException(invalid.getMessage());
        }
        evaluation.setRubricVersion(version);
        evaluation.replaceCriterionScores(version.getCriteria().stream()
                .map(criterion -> new EvaluationCriterionScore(criterion, byCode.get(criterion.getCode())))
                .toList());
        evaluation.setTechnicalGrade(byCode.get("technical"));
        evaluation.setPresentationGrade(byCode.get("presentation"));
        evaluation.setArtifactGrade(byCode.get("artifact"));
        setFinalGrade(evaluation, finalGrade);
    }

    private void applyLegacy(EvaluationRequest request, Evaluation evaluation) {
        var scores = Map.of(
                "technical", request.technicalGrade(),
                "presentation", request.presentationGrade(),
                "artifact", request.artifactGrade());
        var policy = RubricCalculator.legacyThreeCriteriaPolicy();
        try {
            setFinalGrade(evaluation, calculator.calculate(policy, scores));
        } catch (IllegalArgumentException invalid) {
            throw new RubricRuleViolationException(invalid.getMessage());
        }
        evaluation.setRubricVersion(null);
        evaluation.replaceCriterionScores(java.util.List.of());
    }

    private void setFinalGrade(Evaluation evaluation, BigDecimal finalGrade) {
        evaluation.setFinalGrade(finalGrade);
    }

    private void authorize(ExaminationBoard board, plataforma.model.UserAccount evaluator) {
        if (board.getStatus() != plataforma.model.BoardStatus.IN_EVALUATION) {
            throw new RubricRuleViolationException("A banca não está em avaliação");
        }
        if (!evaluator.isActive() || evaluator.getRole() != plataforma.model.Role.PROFESSOR
                || !board.hasEvaluator(evaluator)) {
            throw new EvaluationAccessDeniedException();
        }
        UUID actorId = subjects.currentSubject()
                .map(subject -> parseUuid(subject.id()))
                .orElse(null);
        if (actorId == null || !actorId.equals(evaluator.getId())) {
            throw new EvaluationAccessDeniedException();
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private void consolidate(ExaminationBoard board) {
        var evaluators = board.getEvaluators();
        if (evaluators.isEmpty()) {
            return;
        }
        var evaluations = repository.findAllByBoardId(board.getId());
        var evaluatorIds = evaluations.stream().map(evaluation -> evaluation.getEvaluator().getId()).distinct().toList();
        if (evaluatorIds.size() != evaluators.size() || !evaluatorIds.containsAll(
                evaluators.stream().map(plataforma.model.UserAccount::getId).toList())) {
            return;
        }
        var average = evaluations.stream().map(Evaluation::getFinalGrade)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(evaluations.size()), 2, java.math.RoundingMode.HALF_UP);
        board.setFinalGrade(average);
        boardRepository.saveAndFlush(board);
    }

    private Map<String, Object> snapshot(Evaluation evaluation) {
        var data = new LinkedHashMap<String, Object>();
        data.put("board_id", evaluation.getBoard() == null ? null : evaluation.getBoard().getId());
        data.put("evaluator_id", evaluation.getEvaluator() == null ? null : evaluation.getEvaluator().getId());
        data.put("rubric_version_id", evaluation.getRubricVersion() == null ? null : evaluation.getRubricVersion().getId());
        data.put("technical_grade", evaluation.getTechnicalGrade());
        data.put("presentation_grade", evaluation.getPresentationGrade());
        data.put("artifact_grade", evaluation.getArtifactGrade());
        data.put("final_grade", evaluation.getFinalGrade());
        data.put("feedback", evaluation.getFeedback());
        data.put("criterion_scores", evaluation.getCriterionScores().stream().collect(
                java.util.stream.Collectors.toMap(score -> score.getCriterion().getCode(),
                        EvaluationCriterionScore::getScore, (first, ignored) -> first, LinkedHashMap::new)));
        return data;
    }

    private void audit(AuditOperation operation, UUID recordId, Map<String, Object> oldData,
            Map<String, Object> newData) {
        UUID actorId = subjects.currentSubject().map(subject -> parseUuid(subject.id())).orElse(null);
        auditEventService.record(new plataforma.dto.RecordAuditEventRequest(
                "academic_pi", "evaluation", operation, recordId, actorId, oldData, newData));
    }
}
