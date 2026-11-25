package plataforma.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Datas no formato {@code AAAA-MM-DD}. O edital pode começar e terminar no mesmo dia. */
public record NoticeRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @NotNull @Positive Integer totalHours,
        @NotNull UUID advisorId,
        @NotNull Boolean active) {

    /** Campos nulos já são rejeitados pelo {@code @NotNull}; aqui só se compara o período. */
    @JsonIgnore
    @AssertTrue(message = "endsOn não pode ser anterior a startsOn")
    public boolean isPeriodValid() {
        return startsOn == null || endsOn == null || !endsOn.isBefore(startsOn);
    }
}
