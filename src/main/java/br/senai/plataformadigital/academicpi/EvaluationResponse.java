package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID projectId,
        UUID periodId,
        UUID evaluatedStudentId,
        Instant evaluationDate,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> evaluatorIds) { }
