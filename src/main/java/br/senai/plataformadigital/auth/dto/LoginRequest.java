package br.senai.plataformadigital.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.ToString;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @ToString.Exclude String password
) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=[REDACTED]]";
    }
}
