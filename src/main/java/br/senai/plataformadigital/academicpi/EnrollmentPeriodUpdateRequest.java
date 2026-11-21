package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Changes only the deadline; the period start and academic term are immutable. */
public record EnrollmentPeriodUpdateRequest(@NotNull Instant endsAt) { }
