package br.senai.plataformadigital.extension;

import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(UUID id, String publicCode, UUID projectId, UUID periodId, UUID studentId,
                                  String contentHash, String status, Instant issuedAt, Instant revokedAt,
                                  String validationUrl) { }
