package plataforma.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import plataforma.rubric.RubricFormula;

public record RubricVersionResponse(
        UUID id,
        UUID rubricId,
        String rubricCode,
        String rubricName,
        int version,
        BigDecimal minimumScore,
        BigDecimal maximumScore,
        int roundingScale,
        RoundingMode roundingMode,
        RubricFormula formula,
        List<RubricCriterionResponse> criteria,
        Instant createdAt) {
}
