package plataforma.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricCriterionResponse(
        UUID id,
        String code,
        String name,
        BigDecimal weight,
        BigDecimal minimumScore,
        BigDecimal maximumScore,
        int position) {
}
