package plataforma.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NoticeResponse(
        UUID id,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        int totalHours,
        UUID advisorId,
        boolean active,
        Instant createdAt) {
}
