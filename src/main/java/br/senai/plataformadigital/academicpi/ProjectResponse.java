package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String title,
        String summary,
        String description,
        ProjectClassification classification,
        ProjectContinuity continuity,
        LocalDate endDate,
        ProjectPublicationStatus publicationStatus,
        String lifecycleStatus,
        List<UUID> responsibleTeacherIds,
        Instant createdAt,
        Instant updatedAt) {
}
