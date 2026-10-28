package br.senai.plataformadigital.portal.api.impact;

import java.util.Objects;

/** Referências opacas de uma versão imutável; a origem é interna, não um campo público. */
public record ImpactVersion(String impactId, String versionId, String originReference) {
    public ImpactVersion {
        Objects.requireNonNull(impactId, "Referência de impacto obrigatória");
        Objects.requireNonNull(versionId, "Referência de versão obrigatória");
        Objects.requireNonNull(originReference, "Referência de origem obrigatória");
        if (impactId.isBlank()) {
            throw new IllegalArgumentException("Referência de impacto vazia");
        }
        if (versionId.isBlank()) {
            throw new IllegalArgumentException("Referência de versão vazia");
        }
        if (originReference.isBlank()) {
            throw new IllegalArgumentException("Referência de origem vazia");
        }
    }
}
