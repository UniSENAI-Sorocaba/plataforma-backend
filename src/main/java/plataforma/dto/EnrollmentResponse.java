package plataforma.dto;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID noticeId,
        UUID studentId,
        Instant enrolledAt) {
}
