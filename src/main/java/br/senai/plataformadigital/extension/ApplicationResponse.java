package br.senai.plataformadigital.extension;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(UUID id, UUID opportunityId, UUID studentId,
                                  ApplicationStatus status, Instant submittedAt, Instant withdrawnAt) { }
