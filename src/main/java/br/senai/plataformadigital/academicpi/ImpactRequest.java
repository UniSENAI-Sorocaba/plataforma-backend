package br.senai.plataformadigital.academicpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

public record ImpactRequest(@NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 10, max = 1000) String summary,
        @Size(max = 1000) String evidenceUrl,
        @Min(0) Integer beneficiariesCount,
        @DecimalMin(value = "0.01") BigDecimal activityHours,
        @Min(0) Integer partnersCount,
        @Size(max = 200) String territory,
        @Size(max = 1000) String beneficiarySummary,
        List<@Size(max = 20) String> odsCodes) {
    public ImpactRequest(String title, String summary, String evidenceUrl) {
        this(title, summary, evidenceUrl, null, null, null, null, null, List.of());
    }
}
