package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectRequest(
        @Size(min = 5, max = 200) String title,
        @Size(min = 20, max = 500) String summary,
        @Size(min = 50, max = 5000) String description,
        @NotNull ProjectClassification classification,
        @NotNull ProjectContinuity continuity,
        LocalDate endDate,
        @NotNull ProjectPublicationStatus publicationStatus,
        @NotEmpty List<UUID> responsibleTeacherIds) {
}
