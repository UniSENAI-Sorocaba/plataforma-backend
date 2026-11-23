package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
        UUID projectId,
        UUID periodId,
        UUID studentId,
        String status,
        Instant joinedAt,
        Instant removedAt,
        String removalReason,
        String studentName,
        String studentEmail) { }
