package plataforma.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record EvaluationResponse(
        UUID id,
        UUID boardId,
        UUID evaluatorId,
        BigDecimal technicalGrade,
        BigDecimal presentationGrade,
        BigDecimal artifactGrade,
        BigDecimal finalGrade,
        String feedback,
        Instant createdAt,
        UUID rubricVersionId,
        List<EvaluationCriterionScoreResponse> criterionScores) {
}
