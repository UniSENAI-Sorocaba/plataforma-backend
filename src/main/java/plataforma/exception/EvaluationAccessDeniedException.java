package plataforma.exception;

/** O avaliador autenticado não pode lançar ou alterar a avaliação informada. */
public class EvaluationAccessDeniedException extends RuntimeException {
    public EvaluationAccessDeniedException() {
        super("Avaliador não autorizado para esta banca");
    }
}
