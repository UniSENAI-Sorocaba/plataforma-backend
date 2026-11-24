package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.UUID;

public record DeliverableResponse(
        UUID id,
        UUID projectId,
        UUID periodId,
        UUID ownerId,
        String fileName,
        String contentType,
        long sizeBytes,
        String sha256,
        int version,
        DeliverableStatus status,
        Instant createdAt,
        Instant uploadedAt,
        Instant uploadExpiresAt,
        String uploadUrl,
        String downloadUrl) { }
