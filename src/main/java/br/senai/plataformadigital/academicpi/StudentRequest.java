package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentRequest(
        @NotBlank @Size(min = 2, max = 255) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @NotBlank @Size(max = 50) String registrationNumber,
        @NotBlank @Size(max = 50) String classCode) { }
