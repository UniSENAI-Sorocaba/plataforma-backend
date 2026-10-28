package br.senai.plataformadigital.portal.api.impact;

import java.util.Optional;

/**
 * Porta para a fotografia persistida selecionada pelo fluxo de publicação.
 * O adapter deve garantir coerência de versão/origem/aprovação e atualidade da retirada.
 * Não deve construir aprovação a partir de claims/campos arbitrários do cliente.
 */
@FunctionalInterface
public interface ImpactSnapshotReader {
    Optional<ImpactPublicationSnapshot> findPublication(String impactId);
}
