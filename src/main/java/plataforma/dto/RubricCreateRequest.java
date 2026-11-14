package plataforma.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RubricCreateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull @Valid RubricVersionRequest version) {
}
