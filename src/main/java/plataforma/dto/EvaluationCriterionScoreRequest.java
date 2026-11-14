package plataforma.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EvaluationCriterionScoreRequest(
        @NotBlank @Size(max = 80) String criterionCode,
        @NotNull @Digits(integer = 6, fraction = 4) BigDecimal score) {
}
