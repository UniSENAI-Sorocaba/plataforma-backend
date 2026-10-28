package br.senai.plataformadigital.portal.api.impact;

/**
 * Resultado mínimo da seleção técnica: não contém origem, aprovação ou dados privados.
 * Não é o DTO público final do CONTRACT-PORTAL-001 nem contém métricas institucionais.
 */
public record PublicImpactReference(String impactId, String versionId) {
}
