package br.senai.plataformadigital.extension;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record OpportunityRequest(
        UUID projectId,
        @NotBlank @Size(min = 5, max = 200) String title,
        @NotBlank @Size(min = 20, max = 5000) String description,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) { }
