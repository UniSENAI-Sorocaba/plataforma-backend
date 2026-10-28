package br.senai.plataformadigital.portal.api.impact;

import java.util.Objects;

/** Referência à versão cuja aprovação deve ter sido validada pelo adapter confiável. */
public record ImpactApproval(String impactId, String versionId) {
    public ImpactApproval {
        Objects.requireNonNull(impactId, "Referência de impacto obrigatória");
        Objects.requireNonNull(versionId, "Referência de versão obrigatória");
        if (impactId.isBlank()) {
            throw new IllegalArgumentException("Referência de impacto vazia");
        }
        if (versionId.isBlank()) {
            throw new IllegalArgumentException("Referência de versão vazia");
        }
    }
}
