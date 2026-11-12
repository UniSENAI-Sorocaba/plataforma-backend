package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateResponse;
import plataforma.dto.CertificateRevocationRequest;
import plataforma.dto.PageResponse;
import plataforma.service.CertificateService;

/**
 * Gestão de certificados. Não há PUT nem DELETE: o conteúdo emitido é imutável e um certificado
 * cancelado é revogado, não apagado.
 */
@RestController
@ConditionalOnProperty(prefix = "platform.crud.api", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/certificates")
@Tag(name = "Certificados")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emite o certificado de uma inscrição com horas aprovadas")
    public CertificateResponse issue(@Valid @RequestBody CertificateRequest request) {
        return service.issue(request);
    }

    @GetMapping
    @Operation(summary = "Lista certificados (paginado)")
    public PageResponse<CertificateResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um certificado pelo id")
    public CertificateResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/revocation")
    @Operation(summary = "Revoga um certificado, mantendo o registro")
    public CertificateResponse revoke(@PathVariable UUID id,
            @Valid @RequestBody CertificateRevocationRequest request) {
        return service.revoke(id, request);
    }

    @GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "QR Code (PNG) com o link de validação pública")
    public byte[] qrCode(@PathVariable UUID id) {
        return service.qrCode(id);
    }
}
