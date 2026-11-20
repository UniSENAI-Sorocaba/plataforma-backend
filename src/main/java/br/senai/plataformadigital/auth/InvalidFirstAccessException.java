package br.senai.plataformadigital.auth;

public final class InvalidFirstAccessException extends RuntimeException {
    public InvalidFirstAccessException() {
        super("Não foi possível ativar o primeiro acesso.");
    }
}
