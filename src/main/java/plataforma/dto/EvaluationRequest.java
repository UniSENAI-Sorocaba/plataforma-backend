package plataforma.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Notas de 0 a 10 para cada critério da rubrica, com até duas casas decimais. O limite de casas
 * evita que o banco ({@code numeric(4,2)}) arredonde a nota sem avisar.
 */
public record EvaluationRequest(
        @NotNull UUID boardId,
        @NotNull UUID evaluatorId,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal technicalGrade,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal presentationGrade,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal artifactGrade,
        @Size(max = 4000) String feedback) {
}
