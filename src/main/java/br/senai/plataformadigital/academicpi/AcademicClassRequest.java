package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcademicClassRequest(@NotBlank @Size(max = 50) String code,
                                   @NotBlank @Size(max = 255) String name,
                                   @NotBlank @Size(max = 20) String term) { }
