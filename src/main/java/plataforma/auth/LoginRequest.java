package plataforma.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @NotBlank String password
) {
    @Override
    public String toString() {
        return "LoginRequest[cpf=[REDACTED], password=[REDACTED]]";
    }
}
