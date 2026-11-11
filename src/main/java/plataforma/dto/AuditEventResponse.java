package plataforma.dto;

import plataforma.model.AuditOperation;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        String schemaName,
        String tableName,
        AuditOperation operation,
        UUID recordId,
        UUID actorId,
        Map<String, Object> oldData,
        Map<String, Object> newData) {
}
