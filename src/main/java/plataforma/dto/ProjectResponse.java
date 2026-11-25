package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.ProjectStatus;
import plataforma.model.ProjectType;

public record ProjectResponse(
        UUID id,
        String title,
        String description,
        ProjectType projectType,
        ProjectStatus status,
        String semester,
        String course,
        UUID leaderId,
        UUID advisorId,
        Instant createdAt,
        Instant updatedAt) {
}
