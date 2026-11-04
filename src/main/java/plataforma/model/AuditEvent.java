package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evento de auditoria append-only mapeado para {@code public.audit_event} (DB-001).
 * Não possui setters: uma vez registrado, o evento não é alterado.
 */
@Entity
@Immutable
@Table(schema = "public", name = "audit_event")
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "schema_name", nullable = false, updatable = false, length = 63)
    private String schemaName;

    @Column(name = "table_name", nullable = false, updatable = false, length = 63)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, updatable = false, length = 10)
    private AuditOperation operation;

    @Column(name = "record_id", updatable = false)
    private UUID recordId;

    /** Referência lógica ao usuário de {@code auth}; ainda sem FK até DB-004. */
    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> newData;

    protected AuditEvent() {
        // exigido pela JPA
    }

    private AuditEvent(
            UUID id,
            Instant occurredAt,
            String schemaName,
            String tableName,
            AuditOperation operation,
            UUID recordId,
            UUID actorId,
            Map<String, Object> oldData,
            Map<String, Object> newData) {
        this.id = Objects.requireNonNull(id, "id");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.schemaName = Objects.requireNonNull(schemaName, "schemaName");
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.recordId = recordId;
        this.actorId = actorId;
        this.oldData = oldData;
        this.newData = newData;
    }

    public static AuditEvent record(
            UUID id,
            Instant occurredAt,
            String schemaName,
            String tableName,
            AuditOperation operation,
            UUID recordId,
            UUID actorId,
            Map<String, Object> oldData,
            Map<String, Object> newData) {
        return new AuditEvent(id, occurredAt, schemaName, tableName, operation, recordId, actorId, oldData, newData);
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Map<String, Object> getOldData() {
        return oldData;
    }

    public Map<String, Object> getNewData() {
        return newData;
    }
}
