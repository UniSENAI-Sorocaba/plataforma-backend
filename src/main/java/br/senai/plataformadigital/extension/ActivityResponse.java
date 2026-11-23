package br.senai.plataformadigital.extension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(UUID id, UUID projectId, UUID periodId, UUID studentId,
                               String studentName, String studentEmail,
                               String title, String description, BigDecimal requestedHours,
                               BigDecimal approvedHours, ActivityStatus status, String rejectionReason,
                               Instant submittedAt, Instant reviewedAt, UUID reviewedBy, Instant updatedAt) { }
