package plataforma.dto;

import java.math.BigDecimal;

public record EvaluationCriterionScoreResponse(String criterionCode, BigDecimal score) {
}
