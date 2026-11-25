package plataforma.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CertificateRequest(
        @NotNull UUID enrollmentId,
        @NotNull @Positive Integer hours) {
}
