package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImpactRequest(@NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 10, max = 1000) String summary,
        @Size(max = 1000) String evidenceUrl) { }
