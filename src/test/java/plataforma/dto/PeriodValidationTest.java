package plataforma.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import plataforma.model.BoardStatus;

class PeriodValidationTest {

    private static final Instant START = Instant.parse("2026-11-20T14:00:00Z");
    private static final LocalDate DAY = LocalDate.parse("2026-10-01");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bancaPrecisaTerminarDepoisDeComecar() {
        assertThat(validator.validate(board(START.plusSeconds(3600)))).isEmpty();
        assertThat(validator.validate(board(START))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("periodValid");
        assertThat(validator.validate(board(START.minusSeconds(3600)))).isNotEmpty();
    }

    @Test
    void editalNaoPodeTerminarAntesDeComecar() {
        assertThat(validator.validate(notice(DAY.plusDays(30)))).isEmpty();
        assertThat(validator.validate(notice(DAY))).isEmpty();
        assertThat(validator.validate(notice(DAY.minusDays(1)))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("periodValid");
    }

    private static ExaminationBoardRequest board(Instant endsAt) {
        return new ExaminationBoardRequest(UUID.randomUUID(), START, endsAt, "Sala 101", BoardStatus.SCHEDULED);
    }

    private static NoticeRequest notice(LocalDate endsOn) {
        return new NoticeRequest("Edital", "Descrição", DAY, endsOn, 40, UUID.randomUUID(), true);
    }
}
