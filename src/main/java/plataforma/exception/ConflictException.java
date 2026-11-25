package plataforma.exception;
/** Signals a business conflict without exposing internal persistence details. */
public class ConflictException extends RuntimeException { public ConflictException(String message) { super(message); } }
