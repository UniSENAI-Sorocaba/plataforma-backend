package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import plataforma.dto.CertificateValidationResponse;
import plataforma.service.CertificateService;

/**
 * Validação pública de certificados (destino do QR Code). Não exige login. Fica desligada no perfil
 * padrão até a persistência oficial (DB-013) e o catálogo de rotas públicas (BE-010) existirem.
 */
@RestController
@ConditionalOnProperty(prefix = "platform.certificate.validation", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/public/certificates")
@Tag(name = "Certificados (validação pública)")
public class PublicCertificateController {

    private final CertificateService service;

    public PublicCertificateController(CertificateService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Valida um certificado pelo id e hash impressos no documento",
            description = "Responde apenas situação e datas. Id inexistente ou hash divergente retornam 404.")
    public ResponseEntity<CertificateValidationResponse> validate(@PathVariable UUID id,
            @Parameter(description = "SHA-256 (hex) impresso no certificado") @RequestParam String hash) {
        // no-store: uma revogação precisa aparecer na próxima consulta, sem cache intermediário.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.validate(id, hash));
    }
}
