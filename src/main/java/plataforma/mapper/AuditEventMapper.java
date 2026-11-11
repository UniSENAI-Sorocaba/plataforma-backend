package plataforma.mapper;

import plataforma.dto.AuditEventResponse;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.model.AuditEvent;
import plataforma.security.SecretRedactor;
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
                SecretRedactor.redactMap(request.oldData()),
                SecretRedactor.redactMap(request.newData()));
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
                SecretRedactor.redactMap(event.getOldData()),
                SecretRedactor.redactMap(event.getNewData()));
    }
}
