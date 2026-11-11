package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import plataforma.dto.PageResponse;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateResponse;
import plataforma.service.CertificateService;

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
    @Operation(summary = "Cria um certificado")
    public CertificateResponse create(@Valid @RequestBody CertificateRequest request) {
        return service.create(request);
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

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um certificado")
    public CertificateResponse update(@PathVariable UUID id, @Valid @RequestBody CertificateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um certificado")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
