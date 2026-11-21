package br.senai.plataformadigital.extension;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/certificates")
@ConditionalOnProperty(name = "spring.datasource.url")
public class PublicCertificateController {
    private final CertificateService service;
    public PublicCertificateController(CertificateService service) { this.service = service; }
    @GetMapping("/{code}")
    Map<String, Object> validate(@PathVariable String code) {
        CertificateResponse certificate = service.validate(code);
        return Map.of("valid", "ACTIVE".equals(certificate.status()), "certificate", certificate);
    }
}
