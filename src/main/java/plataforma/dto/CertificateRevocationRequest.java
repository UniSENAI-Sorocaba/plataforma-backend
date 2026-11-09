package plataforma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O motivo fica registrado internamente; a validação pública mostra apenas que o certificado foi revogado. */
public record CertificateRevocationRequest(@NotBlank @Size(max = 500) String reason) {
}
