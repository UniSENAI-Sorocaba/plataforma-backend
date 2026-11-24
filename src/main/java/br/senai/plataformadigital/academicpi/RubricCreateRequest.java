package br.senai.plataformadigital.academicpi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RubricCreateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String name,
        @NotEmpty List<@Valid RubricCriterionRequest> criteria) { }
