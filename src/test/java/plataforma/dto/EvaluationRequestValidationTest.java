package plataforma.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceitaNotaComDuasCasasDecimais() {
        assertThat(validator.validate(request("8.75"))).isEmpty();
    }

    @Test
    void rejeitaNotaComMaisDeDuasCasasDecimais() {
        assertThat(validator.validate(request("8.555")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("technicalGrade");
    }

    @Test
    void rejeitaNotaForaDaFaixa() {
        assertThat(validator.validate(request("10.5"))).isNotEmpty();
    }

    private static EvaluationRequest request(String technicalGrade) {
        return new EvaluationRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(technicalGrade),
                new BigDecimal("9"), new BigDecimal("7"), null);
    }
}
