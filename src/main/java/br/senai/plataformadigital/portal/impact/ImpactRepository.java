package br.senai.plataformadigital.portal.impact;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência para DB-015. Estado, versões e eventos/outbox são
 * atômicos; replace executa compare-and-set da revision. Leitura deve refletir
 * retirada sem cache obsoleto. Eventos append-only serão entregues à BE-016.
 */
public interface ImpactRepository {
    Optional<Impact> find(UUID id);
    boolean create(Impact impact);
    boolean replace(UUID id, long expectedRevision, Impact impact);
}
