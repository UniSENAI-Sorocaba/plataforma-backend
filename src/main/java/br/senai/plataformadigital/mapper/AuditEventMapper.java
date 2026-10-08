package br.senai.plataformadigital.mapper;

import br.senai.plataformadigital.dto.AuditEventResponse;
import br.senai.plataformadigital.dto.RecordAuditEventRequest;
import br.senai.plataformadigital.model.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Única fronteira de conversão entre entidade de persistência e DTOs. */
@Component
public class AuditEventMapper {

    public AuditEvent toEntity(RecordAuditEventRequest request, UUID id, Instant occurredAt) {
        return AuditEvent.record(
                id,
                occurredAt,
                request.schemaName(),
                request.tableName(),
                request.operation(),
                request.recordId(),
                request.actorId(),
                request.oldData(),
                request.newData());
    }

    public AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getSchemaName(),
                event.getTableName(),
                event.getOperation(),
                event.getRecordId(),
                event.getActorId(),
                event.getOldData(),
                event.getNewData());
    }
}
