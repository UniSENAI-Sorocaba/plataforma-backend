package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.ReviewStatus;

public record ActivityResponse(
        UUID id,
        UUID enrollmentId,
        String title,
        String description,
        int hoursRequested,
        Integer hoursApproved,
        String evidenceKey,
        ReviewStatus status,
        String decisionReason,
        UUID reviewerId,
        Instant submittedAt,
        Instant reviewedAt) {
}
