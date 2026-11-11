package plataforma.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnrollmentRequest(
        @NotNull UUID noticeId,
        @NotNull UUID studentId) {
}
