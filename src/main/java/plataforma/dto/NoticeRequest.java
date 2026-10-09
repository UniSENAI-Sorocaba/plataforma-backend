package plataforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Datas no formato {@code AAAA-MM-DD}. */
public record NoticeRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @NotNull @Positive Integer totalHours,
        @NotNull UUID advisorId,
        @NotNull Boolean active) {
}
