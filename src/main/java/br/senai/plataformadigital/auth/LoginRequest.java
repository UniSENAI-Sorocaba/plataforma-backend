package br.senai.plataformadigital.auth;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(@NotBlank @JsonAlias("email") String identifier, @NotBlank String password) {
}
