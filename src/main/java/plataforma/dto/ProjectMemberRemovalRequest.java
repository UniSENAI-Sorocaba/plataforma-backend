package plataforma.dto;

import jakarta.validation.constraints.NotBlank;

/** AC-PI-001-13: a remoção só conclui com justificativa. */
public record ProjectMemberRemovalRequest(@NotBlank String justification) {
}
