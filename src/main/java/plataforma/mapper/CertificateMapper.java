package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateResponse;
import plataforma.model.Certificate;

@Component
public class CertificateMapper {

    public CertificateResponse toResponse(Certificate certificate) {
        return new CertificateResponse(
                certificate.getId(),
                certificate.getEnrollment().getId(),
                certificate.getHours(),
                certificate.getValidationHash(),
                certificate.getIssuedAt());
    }

    /** Copia os campos simples. A inscrição é resolvida no service. */
    public void copyToEntity(CertificateRequest request, Certificate certificate) {
        certificate.setHours(request.hours());
    }
}
