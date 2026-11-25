package plataforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** {@code evidenceKey} referencia o arquivo de comprovação no armazenamento. */
public record ActivityRequest(
        @NotNull UUID enrollmentId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        @NotNull @Positive Integer hoursRequested,
        @NotBlank @Size(max = 512) String evidenceKey) {
}
