package br.senai.plataformadigital.dto;

import br.senai.plataformadigital.model.AuditOperation;
import java.time.Instant;
import java.util.UUID;

/** Critérios opcionais de consulta; {@code occurredTo} é exclusivo. */
public record AuditEventFilter(
        String schemaName,
        String tableName,
        AuditOperation operation,
        UUID recordId,
        UUID actorId,
        Instant occurredFrom,
        Instant occurredTo) {
}
