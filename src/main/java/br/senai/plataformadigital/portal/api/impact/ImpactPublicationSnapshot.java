package br.senai.plataformadigital.portal.api.impact;

import java.util.Objects;
import java.util.Optional;

/**
 * Fotografia interna fornecida por fonte confiável. Aprovação não implica publicação.
 * O flag de retirada sempre impede a projeção, mesmo se published ainda for true.
 * Não representa DDL, comando autorizado ou máquina de estados institucional.
 */
public record ImpactPublicationSnapshot(
        ImpactVersion version, Optional<ImpactApproval> approval, boolean published, boolean withdrawn) {
    public ImpactPublicationSnapshot {
        Objects.requireNonNull(version, "Versão obrigatória");
        Objects.requireNonNull(approval, "Contêiner de aprovação obrigatório");
    }

    public static ImpactPublicationSnapshot draft(ImpactVersion version) {
        return new ImpactPublicationSnapshot(version, Optional.empty(), false, false);
    }

    /** Cópia retirada preserva as referências internas; persistência/histórico dependem do adapter. */
    public ImpactPublicationSnapshot withdraw() {
        return new ImpactPublicationSnapshot(version, approval, published, true);
    }
}
