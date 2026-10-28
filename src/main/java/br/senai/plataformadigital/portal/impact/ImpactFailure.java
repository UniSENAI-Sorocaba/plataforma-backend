package br.senai.plataformadigital.portal.impact;

/** Mensagens públicas fixas: nunca incluir payload, origem, token ou exceção do adapter. */
public final class ImpactFailure extends RuntimeException {
    private final int status;

    public ImpactFailure(int status, String detail) {
        super(detail);
        this.status = status;
    }

    public int status() { return status; }
    public static ImpactFailure unavailable() { return new ImpactFailure(503, "Serviço de impacto indisponível"); }
    public static ImpactFailure missing() { return new ImpactFailure(404, "Impacto ou versão não encontrado"); }
    public static ImpactFailure stale() { return new ImpactFailure(412, "A revisão foi alterada; consulte novamente antes de gravar"); }
}
