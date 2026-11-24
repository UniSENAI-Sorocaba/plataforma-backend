package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class EvaluationService {
    private final EvaluationRepository repository;

    public EvaluationService(EvaluationRepository repository) { this.repository = repository; }

    @Transactional
    public EvaluationResponse create(UUID projectId, UUID periodId, EvaluationRequest request, UUID actor) {
        requireCoordinator(actor);
        if (request.evaluatorIds().stream().distinct().count() != request.evaluatorIds().size()) {
            throw new ConflictException("Avaliadores duplicados.");
        }
        if (!repository.isActiveTarget(projectId, periodId, request.evaluatedStudentId())) {
            throw new ConflictException("O aluno avaliado não possui vínculo ativo no projeto e período.");
        }
        request.evaluatorIds().forEach(evaluator -> {
            if (evaluator.equals(request.evaluatedStudentId())) {
                throw new ConflictException("O aluno não pode avaliar a si próprio.");
            }
            if (!repository.isEligibleEvaluator(projectId, periodId, evaluator)) {
                throw new ConflictException("Avaliador sem vínculo ativo com o projeto e período.");
            }
        });
        try {
            UUID id = repository.insert(projectId, periodId, request, actor);
            request.evaluatorIds().forEach(evaluator -> repository.assign(id, evaluator, actor));
            return repository.findAll(projectId, periodId).stream()
                    .filter(evaluation -> evaluation.id().equals(id)).findFirst()
                    .orElseThrow(() -> new ConflictException("Avaliação criada, mas não pôde ser consultada."));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("O vínculo da avaliação é inválido ou já foi atribuído.");
        }
    }

    public List<EvaluationResponse> list(UUID projectId, UUID periodId, UUID actor) {
        if (!repository.isCoordinator(actor) && !repository.isResponsible(projectId, actor)) {
            throw new ConflictException("Usuário fora do escopo de avaliação do projeto.");
        }
        return repository.findAll(projectId, periodId);
    }

    public List<EvaluationResponse> listAssigned(UUID actor) {
        return repository.findAssigned(actor);
    }

    public List<EvaluationResponse> listForStudent(UUID actor) {
        return repository.findForStudent(actor);
    }

    public List<CoordinationEvaluationResponse> listForCoordination(UUID actor) {
        if (!repository.isCoordinator(actor)) throw new ConflictException("A operação exige papel de coordenação.");
        return repository.findAllForCoordination();
    }

    @Transactional
    public EvaluationGradeResponse grade(UUID projectId, UUID periodId, UUID evaluationId,
                                          EvaluationGradeRequest request, UUID actor) {
        if (!repository.isAssignedEvaluator(evaluationId, actor)) {
            throw new ConflictException("O usuário não está atribuído a esta avaliação.");
        }
        var rubric = repository.findRubric(request.rubricVersionId());
        var score = RubricService.calculate(rubric.criteria(), request.scores());
        if (!repository.belongsTo(evaluationId, projectId, periodId)) {
            throw new ConflictException("Avaliação fora do projeto ou período informado.");
        }
        repository.saveGrade(evaluationId, request.rubricVersionId(), request.scores(), rubric.criteria(), score);
        return new EvaluationGradeResponse(evaluationId, request.rubricVersionId(), score);
    }

    private void requireCoordinator(UUID actor) {
        if (!repository.isCoordinator(actor)) throw new ConflictException("A atribuição de avaliadores exige coordenação.");
    }
}
