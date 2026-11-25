package br.senai.plataformadigital.portal.impact;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter local/teste, nunca escolhido automaticamente em produção. */
public final class InMemoryImpactRepository implements ImpactRepository {
    private final Map<UUID, Impact> impacts = new HashMap<>();

    @Override
    public synchronized Optional<Impact> find(UUID id) { return Optional.ofNullable(impacts.get(id)); }

    @Override
    public synchronized boolean create(Impact impact) {
        return impacts.putIfAbsent(impact.id(), impact) == null;
    }

    @Override
    public synchronized boolean replace(UUID id, long expectedRevision, Impact impact) {
        Impact existing = impacts.get(id);
        if (existing == null || existing.revision() != expectedRevision) return false;
        impacts.put(id, impact);
        return true;
    }
}
