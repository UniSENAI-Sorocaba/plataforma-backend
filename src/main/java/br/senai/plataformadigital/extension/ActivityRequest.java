package br.senai.plataformadigital.extension;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActivityRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 10, max = 5000) String description,
        @NotNull @DecimalMin("0.25") @DecimalMax("100.00") BigDecimal requestedHours) { }
