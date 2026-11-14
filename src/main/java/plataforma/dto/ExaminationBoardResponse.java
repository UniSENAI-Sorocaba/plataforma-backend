package plataforma.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import plataforma.model.BoardStatus;

public record ExaminationBoardResponse(
        UUID id,
        UUID projectId,
        Instant scheduledAt,
        Instant endsAt,
        String location,
        BoardStatus status,
        BigDecimal finalGrade,
        Instant createdAt,
        List<UUID> evaluatorIds) {
}
