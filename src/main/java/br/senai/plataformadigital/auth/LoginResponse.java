package br.senai.plataformadigital.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
