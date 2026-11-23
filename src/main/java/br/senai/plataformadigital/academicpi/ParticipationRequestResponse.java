package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.UUID;

public record ParticipationRequestResponse(
        UUID id,
        UUID projectId,
        UUID periodId,
        UUID studentId,
        ParticipationRequestStatus status,
        Instant requestedAt,
        Instant decidedAt,
        Instant withdrawnAt,
        String studentName,
        String studentEmail,
        String academicTerm) {
    public ParticipationRequestResponse(UUID id, UUID projectId, UUID periodId, UUID studentId,
            ParticipationRequestStatus status, Instant requestedAt, Instant decidedAt, Instant withdrawnAt) {
        this(id, projectId, periodId, studentId, status, requestedAt, decidedAt, withdrawnAt, null, null, null);
    }
}
