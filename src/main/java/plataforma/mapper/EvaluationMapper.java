package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.EvaluationRequest;
import plataforma.dto.EvaluationResponse;
import plataforma.model.Evaluation;

@Component
public class EvaluationMapper {

    public EvaluationResponse toResponse(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getBoard().getId(),
                evaluation.getEvaluator().getId(),
                evaluation.getTechnicalGrade(),
                evaluation.getPresentationGrade(),
                evaluation.getArtifactGrade(),
                evaluation.getFinalGrade(),
                evaluation.getFeedback(),
                evaluation.getCreatedAt());
    }

    /** Copia os campos simples. Banca e avaliador são resolvidos no service. */
    public void copyToEntity(EvaluationRequest request, Evaluation evaluation) {
        evaluation.setTechnicalGrade(request.technicalGrade());
        evaluation.setPresentationGrade(request.presentationGrade());
        evaluation.setArtifactGrade(request.artifactGrade());
        evaluation.setFeedback(request.feedback());
    }
}
