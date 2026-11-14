package plataforma.rubric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RubricCalculatorTest {

    private final RubricCalculator calculator = new RubricCalculator();

    @Test
    void calculaMediaPonderadaComPrecisaoDecimalEArredondamentoDeterministico() {
        var policy = policy(
                new RubricCriterionDefinition("technical", new BigDecimal("0.20"), BigDecimal.ZERO, BigDecimal.TEN),
                new RubricCriterionDefinition("presentation", new BigDecimal("0.30"), BigDecimal.ZERO, BigDecimal.TEN),
                new RubricCriterionDefinition("artifact", new BigDecimal("0.50"), BigDecimal.ZERO, BigDecimal.TEN));

        var result = calculator.calculate(policy, Map.of(
                "technical", new BigDecimal("8.00"),
                "presentation", new BigDecimal("9.00"),
                "artifact", new BigDecimal("9.99")));

        assertThat(result.scale()).isEqualTo(2);
        assertThat(result).isEqualByComparingTo("9.30");
    }

    @Test
    void exigeExatamenteOsCriteriosDaVersao() {
        var policy = policy(new RubricCriterionDefinition(
                "technical", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN));

        assertThatThrownBy(() -> calculator.calculate(policy, Map.of("other", new BigDecimal("8"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente");
    }

    @Test
    void rejeitaNotaForaDoLimiteDoCriterio() {
        var policy = policy(new RubricCriterionDefinition(
                "technical", BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("8")));

        assertThatThrownBy(() -> calculator.calculate(policy, Map.of("technical", new BigDecimal("8.01"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limite");
    }

    @Test
    void rejeitaSomaDePesosDiferenteDeUm() {
        assertThatThrownBy(() -> policy(
                new RubricCriterionDefinition("technical", new BigDecimal("0.60"), BigDecimal.ZERO, BigDecimal.TEN),
                new RubricCriterionDefinition("artifact", new BigDecimal("0.30"), BigDecimal.ZERO, BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("soma dos pesos");
    }

    @Test
    void preservaResultadoDeCasasMuitoGrandesAntesDoArredondamento() {
        var policy = new RubricCalculationPolicy(
                BigDecimal.ZERO, BigDecimal.TEN, 2, RoundingMode.HALF_UP,
                RubricFormula.WEIGHTED_AVERAGE,
                List.of(new RubricCriterionDefinition("technical", BigDecimal.ONE,
                        BigDecimal.ZERO, BigDecimal.TEN)));

        assertThat(calculator.calculate(policy, Map.of("technical", new BigDecimal("8.555"))))
                .isEqualByComparingTo("8.56");
    }

    private static RubricCalculationPolicy policy(RubricCriterionDefinition... criteria) {
        return new RubricCalculationPolicy(
                BigDecimal.ZERO, BigDecimal.TEN, 2, RoundingMode.HALF_UP,
                RubricFormula.WEIGHTED_AVERAGE, List.of(criteria));
    }
}
