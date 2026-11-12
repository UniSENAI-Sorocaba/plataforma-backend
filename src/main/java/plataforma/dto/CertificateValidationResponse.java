package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.CertificateStatus;

/**
 * Resposta mínima da validação pública. Não inclui aluno, inscrição, horas nem motivo de revogação:
 * horas individuais não são públicas (AC-S3-06) e os campos públicos do certificado ainda não foram
 * aprovados pelo PO (SPEC-EXT-003).
 */
public record CertificateValidationResponse(
        UUID id,
        CertificateStatus status,
        Instant issuedAt,
        Instant revokedAt) {
}
