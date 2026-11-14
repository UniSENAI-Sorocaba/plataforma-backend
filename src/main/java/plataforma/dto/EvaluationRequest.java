package plataforma.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Notas de 0 a 10 para cada critério da rubrica, com até duas casas decimais. O limite de casas
 * evita que o banco ({@code numeric(4,2)}) arredonde a nota sem avisar.
 */
public record EvaluationRequest(
        @NotNull UUID boardId,
        @NotNull UUID evaluatorId,
        @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal technicalGrade,
        @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal presentationGrade,
        @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal artifactGrade,
        @Size(max = 4000) String feedback,
        UUID rubricVersionId,
        @Valid List<@Valid EvaluationCriterionScoreRequest> criterionScores) {

    public EvaluationRequest {
        criterionScores = criterionScores == null ? List.of() : List.copyOf(criterionScores);
    }

    /** Construtor legado preservado para clientes que ainda usam os três critérios fixos. */
    public EvaluationRequest(UUID boardId, UUID evaluatorId, BigDecimal technicalGrade,
            BigDecimal presentationGrade, BigDecimal artifactGrade, String feedback) {
        this(boardId, evaluatorId, technicalGrade, presentationGrade, artifactGrade, feedback, null, List.of());
    }

    @JsonIgnore
    @AssertTrue(message = "Informe rubricVersionId e criterionScores, ou os três critérios legados")
    public boolean hasScorePayload() {
        if (rubricVersionId != null) {
            return !criterionScores.isEmpty();
        }
        return technicalGrade != null && presentationGrade != null && artifactGrade != null;
    }
}
