package plataforma.exception;

/** Regra de domínio rejeitada durante a criação ou uso de uma rubrica. */
public class RubricRuleViolationException extends RuntimeException {
    public RubricRuleViolationException(String message) {
        super(message);
    }
}
