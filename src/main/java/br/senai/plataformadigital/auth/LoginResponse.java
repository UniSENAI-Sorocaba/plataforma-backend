package br.senai.plataformadigital.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {
    public LoginResponse(String accessToken, String tokenType, long expiresIn) {
        this(accessToken, tokenType, expiresIn, null);
    }
}
