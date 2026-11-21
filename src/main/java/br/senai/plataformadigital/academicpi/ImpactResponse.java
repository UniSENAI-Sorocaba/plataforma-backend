package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.UUID;

public record ImpactResponse(UUID id, UUID projectId, String title, ImpactStatus status, int version,
        String summary, String evidenceUrl, Instant updatedAt) { }
