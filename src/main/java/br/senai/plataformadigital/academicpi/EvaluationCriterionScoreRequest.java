package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EvaluationCriterionScoreRequest(
        @NotBlank String criterionCode,
        @NotNull @DecimalMin("0.0") BigDecimal score) { }
