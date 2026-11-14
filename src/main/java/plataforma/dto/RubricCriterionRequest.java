package plataforma.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RubricCriterionRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.00000001") @DecimalMax("1.0") @Digits(integer = 1, fraction = 8)
                BigDecimal weight,
        @NotNull @Digits(integer = 6, fraction = 4) BigDecimal minimumScore,
        @NotNull @Digits(integer = 6, fraction = 4) BigDecimal maximumScore,
        @Min(0) int position) {
}
