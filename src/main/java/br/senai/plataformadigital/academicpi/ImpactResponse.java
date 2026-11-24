package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ImpactResponse(UUID id, UUID projectId, String title, ImpactStatus status, int version,
        String summary, String evidenceUrl, Instant updatedAt, Integer beneficiariesCount,
        BigDecimal activityHours, Integer partnersCount, String territory, String beneficiarySummary,
        List<String> odsCodes) {
    public ImpactResponse(UUID id, UUID projectId, String title, ImpactStatus status, int version,
            String summary, String evidenceUrl, Instant updatedAt) {
        this(id, projectId, title, status, version, summary, evidenceUrl, updatedAt,
                null, null, null, null, null, List.of());
    }
}
