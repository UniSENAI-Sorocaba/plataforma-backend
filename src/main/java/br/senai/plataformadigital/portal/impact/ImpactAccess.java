package br.senai.plataformadigital.portal.impact;

import java.util.Optional;
import java.util.UUID;

/** Identidade e autorização confiáveis do servidor, nunca provenientes do payload. */
public interface ImpactAccess {
    Optional<String> currentActor();
    boolean permits(String actor, Impact.Action action, UUID impactId);
}
