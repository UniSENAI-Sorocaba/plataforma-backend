package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.CertificateResponse;
import plataforma.dto.CertificateValidationResponse;
import plataforma.model.Certificate;

@Component
public class CertificateMapper {

    public CertificateResponse toResponse(Certificate certificate, String validationUrl) {
        return new CertificateResponse(
                certificate.getId(),
                certificate.getEnrollment().getId(),
                certificate.getHours(),
                certificate.getValidationHash(),
                certificate.getStatus(),
                certificate.getIssuedAt(),
                certificate.getRevokedAt(),
                certificate.getRevocationReason(),
                validationUrl);
    }

    public CertificateValidationResponse toValidationResponse(Certificate certificate) {
        return new CertificateValidationResponse(
                certificate.getId(),
                certificate.getStatus(),
                certificate.getIssuedAt(),
                certificate.getRevokedAt());
    }
}
