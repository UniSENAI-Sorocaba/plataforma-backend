package br.senai.plataformadigital.controller;

import br.senai.plataformadigital.dto.AuditEventFilter;
import br.senai.plataformadigital.dto.AuditEventResponse;
import br.senai.plataformadigital.service.AuditEventService;
import br.senai.plataformadigital.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de auditoria. Desligada por padrão até existir RBAC server-side (BE-010),
 * pois os eventos podem conter dados pessoais.
 */
@RestController
@RequestMapping("/api/v1/audit-events")
@ConditionalOnProperty(prefix = "platform.audit.api", name = "enabled", havingValue = "true")
@Tag(name = "Auditoria", description = "Consulta de eventos de auditoria (public.audit_event)")
public class AuditEventController {

    private final AuditEventService service;

    public AuditEventController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista eventos de auditoria com filtros opcionais e paginação")
    public PageResponse<AuditEventResponse> search(
            @ParameterObject AuditEventFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um evento de auditoria pelo id")
    public AuditEventResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }
}
