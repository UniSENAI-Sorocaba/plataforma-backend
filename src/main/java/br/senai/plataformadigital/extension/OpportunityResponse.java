package br.senai.plataformadigital.extension;

import java.time.Instant;
import java.util.UUID;

public record OpportunityResponse(UUID id, UUID projectId, String title, String description,
                                  OpportunityStatus status, Instant startsAt, Instant endsAt,
                                  Instant createdAt, Instant updatedAt) { }
