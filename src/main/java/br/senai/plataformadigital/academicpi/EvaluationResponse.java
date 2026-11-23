package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID projectId,
        UUID periodId,
        String projectTitle,
        String academicTerm,
        UUID evaluatedStudentId,
        Instant evaluationDate,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> evaluatorIds,
        String evaluatedStudentName,
        String evaluatedStudentEmail,
        List<InstitutionalDirectoryEntry> evaluatorDetails,
        UUID rubricVersionId,
        java.math.BigDecimal finalScore) {
    public EvaluationResponse(UUID id, UUID projectId, UUID periodId, UUID evaluatedStudentId,
            Instant evaluationDate, UUID createdBy, Instant createdAt, Instant updatedAt, List<UUID> evaluatorIds) {
        this(id, projectId, periodId, null, null, evaluatedStudentId, evaluationDate, createdBy, createdAt, updatedAt,
                evaluatorIds, null, null, List.of(), null, null);
    }
}
