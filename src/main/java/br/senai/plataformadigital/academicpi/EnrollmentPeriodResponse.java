package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentPeriodResponse(
        UUID id,
        UUID projectId,
        String academicTerm,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt) { }
