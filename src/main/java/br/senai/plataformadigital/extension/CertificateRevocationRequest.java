package br.senai.plataformadigital.extension;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificateRevocationRequest(@NotBlank @Size(min = 3, max = 1000) String reason) { }
