package plataforma.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID enrollmentId,
        int hours,
        String validationHash,
        Instant issuedAt) {
}
