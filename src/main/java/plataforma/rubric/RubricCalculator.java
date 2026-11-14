package plataforma.rubric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Motor puro e determinístico; não usa ponto flutuante nem estado externo. */
@Component
public final class RubricCalculator {

    public BigDecimal calculate(RubricCalculationPolicy policy, Map<String, BigDecimal> scores) {
        Objects.requireNonNull(policy, "Política obrigatória");
        Objects.requireNonNull(scores, "Notas obrigatórias");
        if (policy.formula() != RubricFormula.WEIGHTED_AVERAGE) {
            throw new IllegalArgumentException("Fórmula não suportada: " + policy.formula());
        }

        var definitions = policy.criteria().stream()
                .collect(Collectors.toUnmodifiableMap(RubricCriterionDefinition::code, Function.identity()));
        if (!scores.keySet().equals(definitions.keySet())) {
            throw new IllegalArgumentException("A avaliação deve informar exatamente os critérios da rubrica");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var entry : scores.entrySet()) {
            var score = Objects.requireNonNull(entry.getValue(), "Nota obrigatória");
            var criterion = definitions.get(entry.getKey());
            if (score.compareTo(criterion.minimumScore()) < 0
                    || score.compareTo(criterion.maximumScore()) > 0) {
                throw new IllegalArgumentException("Nota fora do limite do critério: " + entry.getKey());
            }
            total = total.add(score.multiply(criterion.weight()));
        }

        var result = total.setScale(policy.roundingScale(), policy.roundingMode());
        if (result.compareTo(policy.minimumScore()) < 0 || result.compareTo(policy.maximumScore()) > 0) {
            throw new IllegalArgumentException("Resultado fora da escala da rubrica");
        }
        return result;
    }

    public static RubricCalculationPolicy legacyThreeCriteriaPolicy() {
        return new RubricCalculationPolicy(
                BigDecimal.ZERO,
                BigDecimal.TEN,
                2,
                RoundingMode.HALF_UP,
                RubricFormula.WEIGHTED_AVERAGE,
                java.util.List.of(
                        new RubricCriterionDefinition("technical", new BigDecimal("0.333333333333333333"),
                                BigDecimal.ZERO, BigDecimal.TEN),
                        new RubricCriterionDefinition("presentation", new BigDecimal("0.333333333333333333"),
                                BigDecimal.ZERO, BigDecimal.TEN),
                        new RubricCriterionDefinition("artifact", new BigDecimal("0.333333333333333334"),
                                BigDecimal.ZERO, BigDecimal.TEN)));
    }
}
