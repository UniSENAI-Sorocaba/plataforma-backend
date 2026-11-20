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
        Instant withdrawnAt) {
}
