package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RubricCriterionRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String label,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal weight,
        @NotNull @DecimalMin(value = "0.0") BigDecimal minScore,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxScore) { }
