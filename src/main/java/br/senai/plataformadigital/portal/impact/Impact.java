package br.senai.plataformadigital.portal.impact;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Estado imutável; repository deve persistir estado e eventos na mesma transação. */
public record Impact(UUID id, long revision, Map<Long, Version> versions,
                     Long publishedVersion, List<Event> history) {
    public Impact {
        versions = Map.copyOf(versions);
        history = List.copyOf(history);
    }

    public record Content(String title, String summary) { }
    public record Provenance(String sourceReference, String responsibleReference,
                             LocalDate periodStart, LocalDate periodEnd) { }
    public record Approval(String actor, Instant at) { }
    public record Version(long number, Content content, Provenance provenance,
                          String createdBy, Instant createdAt, Approval approval, boolean withdrawn) { }
    /** Evento pronto para o adapter de auditoria BE-016, sem conteúdo ou origem privada. */
    public record Event(UUID impactId, long revision, Action action, long version, String actor,
                        Instant at, String correlationId, String result, String withdrawalReason) { }
    public enum Action { CREATE, REVISE, READ, APPROVE, PUBLISH, WITHDRAW, AUDIT }
}
