package br.senai.plataformadigital.portal.api.impact;

import java.util.Objects;
import java.util.Optional;

/** Seleção defensiva sem cache próprio, endpoint, bean automático ou comando de publicação. */
public final class PublicImpactQueryService {
    private final ImpactSnapshotReader reader;

    public PublicImpactQueryService(ImpactSnapshotReader reader) {
        this.reader = Objects.requireNonNull(reader, "Fonte de publicação obrigatória");
    }

    public Optional<PublicImpactReference> findPublished(String impactId) {
        Objects.requireNonNull(impactId, "Referência de impacto obrigatória");
        if (impactId.isBlank()) {
            throw new IllegalArgumentException("Referência de impacto vazia");
        }
        Optional<ImpactPublicationSnapshot> found;
        try {
            found = reader.findPublication(impactId);
        } catch (RuntimeException unavailable) {
            throw new ImpactPublicationUnavailableException();
        }
        if (found == null) {
            throw new ImpactPublicationUnavailableException();
        }
        return found.filter(snapshot -> visible(impactId, snapshot))
                .map(snapshot -> new PublicImpactReference(snapshot.version().impactId(), snapshot.version().versionId()));
    }

    private boolean visible(String impactId, ImpactPublicationSnapshot snapshot) {
        if (snapshot.withdrawn() || !snapshot.published() || !snapshot.version().impactId().equals(impactId)) {
            return false;
        }
        return snapshot.approval()
                .filter(approval -> approval.impactId().equals(snapshot.version().impactId()))
                .filter(approval -> approval.versionId().equals(snapshot.version().versionId()))
                .isPresent();
    }
}
