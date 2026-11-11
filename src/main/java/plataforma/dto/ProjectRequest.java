package plataforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import plataforma.model.ProjectStatus;
import plataforma.model.ProjectType;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull ProjectType projectType,
        @NotNull ProjectStatus status,
        @NotBlank @Pattern(regexp = "\\d{4}\\.[12]", message = "deve seguir o formato AAAA.S, ex.: 2026.2")
        String semester,
        @NotBlank @Size(max = 120) String course,
        @NotNull UUID leaderId,
        UUID advisorId) {
}
