package br.senai.plataformadigital.dto;

import br.senai.plataformadigital.model.AuditOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

/**
 * Dados para registrar um evento de auditoria. Os módulos devem enviar apenas o mínimo
 * necessário em {@code oldData}/{@code newData}: nunca senhas, tokens ou hashes de credenciais.
 */
public record RecordAuditEventRequest(
        @NotBlank @Size(max = 63) String schemaName,
        @NotBlank @Size(max = 63) String tableName,
        @NotNull AuditOperation operation,
        UUID recordId,
        UUID actorId,
        Map<String, Object> oldData,
        Map<String, Object> newData) {
}
