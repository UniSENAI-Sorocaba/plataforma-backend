package br.senai.plataformadigital.extension;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CertificateRequest(@NotNull UUID studentId) { }
