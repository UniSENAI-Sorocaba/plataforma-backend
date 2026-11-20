package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EnrollmentPeriodRequest(
        @NotBlank String academicTerm,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) { }
