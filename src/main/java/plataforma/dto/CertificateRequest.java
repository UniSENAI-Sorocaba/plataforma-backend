package plataforma.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Pedido de emissão. As horas não vêm do cliente: são a soma das atividades aprovadas da inscrição. */
public record CertificateRequest(@NotNull UUID enrollmentId) {
}
