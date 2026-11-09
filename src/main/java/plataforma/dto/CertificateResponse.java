package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.CertificateStatus;

/** Visão interna do certificado (endpoints de gestão). Não usar na consulta pública. */
public record CertificateResponse(
        UUID id,
        UUID enrollmentId,
        int hours,
        String validationHash,
        CertificateStatus status,
        Instant issuedAt,
        Instant revokedAt,
        String revocationReason,
        String validationUrl) {
}
