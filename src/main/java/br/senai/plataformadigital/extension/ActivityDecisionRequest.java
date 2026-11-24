package br.senai.plataformadigital.extension;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ActivityDecisionRequest(
        @NotNull ActivityStatus status,
        @DecimalMin("0.25") @DecimalMax("100.00") BigDecimal approvedHours,
        String rejectionReason) { }
