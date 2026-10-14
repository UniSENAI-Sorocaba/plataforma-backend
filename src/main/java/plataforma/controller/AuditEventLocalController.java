package plataforma.controller;

import plataforma.dto.AuditEventResponse;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.service.AuditEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro manual de eventos apenas para testes locais via Swagger. Fora do perfil
 * {@code local}, eventos são gravados somente pelos módulos através de {@link AuditEventService}.
 */
@RestController
@Profile("local")
@RequestMapping("/api/v1/local/audit-events")
@Tag(name = "Auditoria (somente local)", description = "Criação manual de eventos para testes")
public class AuditEventLocalController {

    private final AuditEventService service;

    public AuditEventLocalController(AuditEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um evento de auditoria sintético")
    public AuditEventResponse record(@Valid @RequestBody RecordAuditEventRequest request) {
        return service.record(request);
    }
}
