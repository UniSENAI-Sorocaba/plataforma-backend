package br.senai.plataformadigital.repository;

import br.senai.plataformadigital.dto.AuditEventFilter;
import br.senai.plataformadigital.model.AuditEvent;
import org.springframework.data.jpa.domain.Specification;

/** Converte o filtro de consulta em critérios JPA; campos nulos são ignorados. */
public final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEvent> matching(AuditEventFilter filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (filter.schemaName() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("schemaName"), filter.schemaName()));
            }
            if (filter.tableName() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("tableName"), filter.tableName()));
            }
            if (filter.operation() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("operation"), filter.operation()));
            }
            if (filter.recordId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("recordId"), filter.recordId()));
            }
            if (filter.actorId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("actorId"), filter.actorId()));
            }
            if (filter.occurredFrom() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.occurredFrom()));
            }
            if (filter.occurredTo() != null) {
                predicates = cb.and(predicates, cb.lessThan(root.get("occurredAt"), filter.occurredTo()));
            }
            return predicates;
        };
    }
}
