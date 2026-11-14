package plataforma.rubric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Política imutável que acompanha uma versão de rubrica. */
public record RubricCalculationPolicy(
        BigDecimal minimumScore,
        BigDecimal maximumScore,
        int roundingScale,
        RoundingMode roundingMode,
        RubricFormula formula,
        List<RubricCriterionDefinition> criteria) {

    public RubricCalculationPolicy {
        minimumScore = Objects.requireNonNull(minimumScore, "Nota mínima obrigatória");
        maximumScore = Objects.requireNonNull(maximumScore, "Nota máxima obrigatória");
        roundingMode = Objects.requireNonNull(roundingMode, "Modo de arredondamento obrigatório");
        formula = Objects.requireNonNull(formula, "Fórmula obrigatória");
        criteria = List.copyOf(Objects.requireNonNull(criteria, "Critérios obrigatórios"));
        if (minimumScore.compareTo(maximumScore) >= 0) {
            throw new IllegalArgumentException("Limites da escala inválidos");
        }
        if (roundingScale < 0 || roundingScale > 8) {
            throw new IllegalArgumentException("Escala de arredondamento inválida");
        }
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("A rubrica precisa de ao menos um critério");
        }
        var sum = criteria.stream().map(RubricCriterionDefinition::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("A soma dos pesos deve ser exatamente 1");
        }
        var codes = criteria.stream().map(RubricCriterionDefinition::code).toList();
        if (codes.stream().distinct().count() != codes.size()) {
            throw new IllegalArgumentException("Os códigos dos critérios devem ser únicos");
        }
        var policyMinimumScore = minimumScore;
        var policyMaximumScore = maximumScore;
        criteria.forEach(criterion -> {
            if (criterion.minimumScore().compareTo(policyMinimumScore) < 0
                    || criterion.maximumScore().compareTo(policyMaximumScore) > 0) {
                throw new IllegalArgumentException("Limite do critério fora da escala da rubrica");
            }
        });
    }
}
