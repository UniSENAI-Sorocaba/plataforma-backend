package plataforma.rubric;

import java.math.BigDecimal;
import java.util.Objects;

/** Definição imutável de um critério usada pelo motor de cálculo. */
public record RubricCriterionDefinition(
        String code,
        BigDecimal weight,
        BigDecimal minimumScore,
        BigDecimal maximumScore) {

    public RubricCriterionDefinition {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Código do critério obrigatório");
        }
        weight = Objects.requireNonNull(weight, "Peso obrigatório");
        minimumScore = Objects.requireNonNull(minimumScore, "Nota mínima obrigatória");
        maximumScore = Objects.requireNonNull(maximumScore, "Nota máxima obrigatória");
        if (weight.signum() <= 0 || minimumScore.compareTo(maximumScore) >= 0) {
            throw new IllegalArgumentException("Critério inválido");
        }
    }
}
