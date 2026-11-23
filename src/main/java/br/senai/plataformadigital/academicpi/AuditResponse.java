package br.senai.plataformadigital.academicpi;
import java.time.Instant; import java.util.UUID;
public record AuditResponse(UUID id, String action, String targetType, UUID targetId, String details, Instant createdAt, String actorName) { }
