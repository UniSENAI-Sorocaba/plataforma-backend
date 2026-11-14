package plataforma.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import plataforma.rubric.RubricFormula;

public record RubricVersionRequest(
        @NotNull BigDecimal minimumScore,
        @NotNull BigDecimal maximumScore,
        @Min(0) @Max(8) int roundingScale,
        @NotNull RoundingMode roundingMode,
        @NotNull RubricFormula formula,
        @NotEmpty List<@Valid RubricCriterionRequest> criteria) {

    public RubricVersionRequest {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
    }
}
