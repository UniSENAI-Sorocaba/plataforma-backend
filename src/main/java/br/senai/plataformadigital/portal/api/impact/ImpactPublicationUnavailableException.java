package br.senai.plataformadigital.portal.api.impact;

/** Não retorna dados antigos ou mensagens privadas da fonte quando a consulta falha. */
public final class ImpactPublicationUnavailableException extends RuntimeException {
    public ImpactPublicationUnavailableException() {
        super("Consulta de publicação de impacto indisponível");
    }
}
