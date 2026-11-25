package br.senai.plataformadigital.portal.impact;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImpactService {
    private static final Logger LOG = LoggerFactory.getLogger(ImpactService.class);
    static final int WITHDRAWAL_REASON_LIMIT = 512;
    private final ImpactRepository repository;
    private final ImpactAccess access;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public ImpactService(ImpactRepository repository, ImpactAccess access, Clock clock, Supplier<UUID> identifiers) {
        this.repository = repository;
        this.access = access;
        this.clock = clock;
        this.identifiers = identifiers;
    }

    public Impact create(Impact.Content content, Impact.Provenance provenance, String correlation) {
        String actor = authorize(Impact.Action.CREATE, null);
        validate(content, provenance);
        UUID id = identifiers.get();
        Impact.Version version = new Impact.Version(1, content, provenance, actor, clock.instant(), null, false);
        Impact impact = new Impact(id, 1, java.util.Map.of(1L, version), null,
                List.of(event(id, 1, Impact.Action.CREATE, 1, actor, correlation, null)));
        if (!storage("create", () -> repository.create(impact))) throw new ImpactFailure(409, "Referência de impacto já existente");
        return impact;
    }

    public Impact revise(UUID id, long expected, Impact.Content content, Impact.Provenance provenance, String correlation) {
        String actor = authorize(Impact.Action.REVISE, id);
        validate(content, provenance);
        Impact current = expected(id, expected);
        long number = current.versions().keySet().stream().mapToLong(Long::longValue).max().orElseThrow() + 1;
        var versions = new HashMap<>(current.versions());
        versions.put(number, new Impact.Version(number, content, provenance, actor, clock.instant(), null, false));
        return save(current, versions, current.publishedVersion(), Impact.Action.REVISE, number, actor, correlation);
    }

    public Impact approve(UUID id, long number, long expected, String correlation) {
        String actor = authorize(Impact.Action.APPROVE, id);
        Impact current = expected(id, expected);
        Impact.Version version = version(current, number);
        if (version.approval() != null && !version.withdrawn()) throw new ImpactFailure(409, "Versão já aprovada");
        if (actor.equals(version.createdBy())) throw new ImpactFailure(403, "O autor não pode aprovar a própria versão");
        var versions = new HashMap<>(current.versions());
        versions.put(number, new Impact.Version(number, version.content(), version.provenance(),
                version.createdBy(), version.createdAt(), new Impact.Approval(actor, clock.instant()), false));
        return save(current, versions, current.publishedVersion(), Impact.Action.APPROVE, number, actor, correlation);
    }

    public Impact publish(UUID id, long number, long expected, String correlation) {
        String actor = authorize(Impact.Action.PUBLISH, id);
        Impact current = expected(id, expected);
        Impact.Version version = version(current, number);
        if (version.approval() == null || version.withdrawn()) {
            throw new ImpactFailure(409, "A versão precisa ser aprovada antes da publicação");
        }
        if (actor.equals(version.approval().actor())) throw new ImpactFailure(403, "Aprovação e publicação exigem pessoas distintas");
        if (Long.valueOf(number).equals(current.publishedVersion())) throw new ImpactFailure(409, "Versão já publicada");
        return save(current, current.versions(), number, Impact.Action.PUBLISH, number, actor, correlation);
    }

    public Impact withdraw(UUID id, long expected, String reason, String correlation) {
        String actor = authorize(Impact.Action.WITHDRAW, id);
        Impact current = expected(id, expected);
        if (current.publishedVersion() == null) throw new ImpactFailure(409, "Impacto sem publicação ativa");
        if (blank(reason) || reason.length() > WITHDRAWAL_REASON_LIMIT) throw new ImpactFailure(400, "Motivo da retirada inválido");
        Impact.Version version = version(current, current.publishedVersion());
        var versions = new HashMap<>(current.versions());
        versions.put(version.number(), new Impact.Version(version.number(), version.content(), version.provenance(),
                version.createdBy(), version.createdAt(), version.approval(), true));
        return save(current, versions, null, Impact.Action.WITHDRAW, version.number(), actor, correlation, reason.strip());
    }

    public Impact read(UUID id) {
        authorize(Impact.Action.READ, id);
        return load(id);
    }

    public Impact.Version provenance(UUID id, long number) {
        authorize(Impact.Action.AUDIT, id);
        return version(load(id), number);
    }

    public List<Impact.Event> history(UUID id) {
        authorize(Impact.Action.AUDIT, id);
        return load(id).history();
    }

    public Impact.Version publicVersion(UUID id) {
        Impact impact = load(id);
        if (impact.publishedVersion() == null) throw ImpactFailure.missing();
        Impact.Version version = version(impact, impact.publishedVersion());
        if (version.approval() == null || version.withdrawn()) throw ImpactFailure.missing();
        return version;
    }

    private Impact save(Impact current, java.util.Map<Long, Impact.Version> versions, Long published,
                        Impact.Action action, long number, String actor, String correlation) {
        return save(current, versions, published, action, number, actor, correlation, null);
    }

    private Impact save(Impact current, java.util.Map<Long, Impact.Version> versions, Long published,
                        Impact.Action action, long number, String actor, String correlation, String withdrawalReason) {
        long revision = current.revision() + 1;
        var events = new ArrayList<>(current.history());
        events.add(event(current.id(), revision, action, number, actor, correlation, withdrawalReason));
        Impact updated = new Impact(current.id(), revision, versions, published, events);
        if (!storage("replace", () -> repository.replace(current.id(), current.revision(), updated))) throw ImpactFailure.stale();
        return updated;
    }

    private Impact.Event event(UUID id, long revision, Impact.Action action, long version, String actor, String correlation, String withdrawalReason) {
        return new Impact.Event(id, revision, action, version, actor, clock.instant(), correlation, "SUCCESS", withdrawalReason);
    }

    private Impact expected(UUID id, long revision) {
        Impact impact = load(id);
        if (impact.revision() != revision) throw ImpactFailure.stale();
        return impact;
    }

    private Impact load(UUID id) {
        Optional<Impact> found = storage("find", () -> repository.find(id));
        if (found == null) throw ImpactFailure.unavailable();
        Impact impact = found.orElseThrow(ImpactFailure::missing);
        if (!id.equals(impact.id())) throw ImpactFailure.unavailable();
        return impact;
    }

    private Impact.Version version(Impact impact, long number) {
        Impact.Version result = impact.versions().get(number);
        if (result == null) throw ImpactFailure.missing();
        if (result.number() != number) throw ImpactFailure.unavailable();
        return result;
    }

    private String authorize(Impact.Action action, UUID id) {
        Optional<String> actor;
        boolean allowed;
        try {
            actor = access.currentActor();
            if (actor == null) throw ImpactFailure.unavailable();
            if (actor.isEmpty() || actor.orElseThrow().isBlank()) throw new ImpactFailure(401, "Autenticação necessária");
            allowed = access.permits(actor.orElseThrow(), action, id);
        } catch (ImpactFailure failure) {
            throw failure;
        } catch (RuntimeException unavailable) {
            throw ImpactFailure.unavailable();
        }
        if (!allowed) throw new ImpactFailure(403, "Acesso negado");
        return actor.orElseThrow();
    }

    private <T> T storage(String name, Supplier<T> operation) {
        try { return operation.get(); }
        catch (RuntimeException unavailable) {
            Throwable cause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(unavailable);
            LOG.warn("Impact repository unavailable during {} ({})", name, cause.getClass().getName());
            throw ImpactFailure.unavailable();
        }
    }

    private void validate(Impact.Content content, Impact.Provenance provenance) {
        if (content == null || provenance == null || blank(content.title()) || blank(content.summary())
                || blank(provenance.sourceReference()) || blank(provenance.responsibleReference())
                || provenance.periodStart() == null || provenance.periodEnd() == null
                || provenance.periodEnd().isBefore(provenance.periodStart())) {
            throw new ImpactFailure(400, "Conteúdo e origem completos com período válido são obrigatórios");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
