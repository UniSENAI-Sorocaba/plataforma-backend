package plataforma.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Notas de 0 a 10 para cada critério da rubrica. */
public record EvaluationRequest(
        @NotNull UUID boardId,
        @NotNull UUID evaluatorId,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal technicalGrade,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal presentationGrade,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal artifactGrade,
        @Size(max = 4000) String feedback) {
}
