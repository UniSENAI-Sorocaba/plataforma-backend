package plataforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import plataforma.model.BoardStatus;

/** Horários em ISO-8601 com fuso, ex.: {@code 2026-11-20T14:00:00-03:00}. */
public record ExaminationBoardRequest(
        @NotNull UUID projectId,
        @NotNull Instant scheduledAt,
        @NotNull Instant endsAt,
        @NotBlank @Size(max = 255) String location,
        @NotNull BoardStatus status) {
}
