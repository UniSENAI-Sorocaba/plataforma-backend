package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotNull;

public record ParticipationDecisionRequest(@NotNull ParticipationRequestStatus status) {
    public ParticipationDecisionRequest {
        if (status != ParticipationRequestStatus.APPROVED && status != ParticipationRequestStatus.DENIED) {
            throw new IllegalArgumentException("A decisão deve ser APPROVED ou DENIED.");
        }
    }
}
