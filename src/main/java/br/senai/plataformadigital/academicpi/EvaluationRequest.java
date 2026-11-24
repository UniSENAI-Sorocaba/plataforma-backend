package br.senai.plataformadigital.academicpi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvaluationRequest(
        @NotNull UUID evaluatedStudentId,
        @NotNull Instant evaluationDate,
        @NotEmpty List<@Valid @NotNull UUID> evaluatorIds) { }
