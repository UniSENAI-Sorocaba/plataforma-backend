package br.senai.plataformadigital.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID PERIOD = UUID.randomUUID();
    private static final UUID ACTIVITY = UUID.randomUUID();

    @Mock ActivityRepository repository;
    private ActivityService service;

    @BeforeEach
    void setUp() { service = new ActivityService(repository, Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test
    void submitsActivityBeforeDeadlineForActiveParticipant() {
        ActivityRequest request = request("Visita técnica", "Relato detalhado da atividade realizada.", "4.00");
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW.plusSeconds(60));
        when(repository.insert(PROJECT, PERIOD, STUDENT, request)).thenReturn(ACTIVITY);
        when(repository.find(ACTIVITY)).thenReturn(activity(ActivityStatus.SUBMITTED, null, null));

        assertThat(service.submit(PROJECT, PERIOD, request, STUDENT).status()).isEqualTo(ActivityStatus.SUBMITTED);
    }

    @Test
    void rejectsInactiveParticipantAndClosedPeriod() {
        ActivityRequest request = request("Visita técnica", "Relato detalhado da atividade realizada.", "4.00");
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(false);
        assertThatThrownBy(() -> service.submit(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("participante");

        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW);
        assertThatThrownBy(() -> service.submit(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("prazo");
        verify(repository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidHourIncrementAndDuplicateActivity() {
        ActivityRequest invalid = request("Visita técnica", "Relato detalhado da atividade realizada.", "1.10");
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW.plusSeconds(60));
        assertThatThrownBy(() -> service.submit(PROJECT, PERIOD, invalid, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("incrementos");

        ActivityRequest valid = request("Visita técnica", "Relato detalhado da atividade realizada.", "1.25");
        when(repository.insert(PROJECT, PERIOD, STUDENT, valid)).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.submit(PROJECT, PERIOD, valid, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("título");
    }

    @Test
    void correctsRejectedActivityBeforeDeadlineAndRejectsOtherOwner() {
        ActivityResponse rejected = activity(ActivityStatus.REJECTED, null, "Falta detalhamento");
        when(repository.find(ACTIVITY)).thenReturn(rejected);
        assertThatThrownBy(() -> service.correct(ACTIVITY, request("Outro título", "Descrição suficiente para correção.", "2.00"), TEACHER))
                .isInstanceOf(ConflictException.class).hasMessageContaining("escopo");

        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW.plusSeconds(60));
        when(repository.updateSubmission(eq(ACTIVITY), eq(STUDENT), any())).thenReturn(1);
        when(repository.find(ACTIVITY)).thenReturn(rejected, activity(ActivityStatus.SUBMITTED, null, null));
        assertThat(service.correct(ACTIVITY, request("Visita técnica", "Descrição suficiente para correção.", "2.00"), STUDENT).status())
                .isEqualTo(ActivityStatus.SUBMITTED);
    }

    @Test
    void cannotCorrectAfterDeadlineOrApprovedActivity() {
        when(repository.find(ACTIVITY)).thenReturn(activity(ActivityStatus.REJECTED, null, "Falta detalhamento"));
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW);
        assertThatThrownBy(() -> service.correct(ACTIVITY, request("Visita técnica", "Descrição suficiente para correção.", "2.00"), STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("prazo");

        when(repository.periodEndsAt(PROJECT, PERIOD)).thenReturn(NOW.plusSeconds(60));
        when(repository.updateSubmission(ACTIVITY, STUDENT, request("Visita técnica", "Descrição suficiente para correção.", "2.00"))).thenReturn(0);
        assertThatThrownBy(() -> service.correct(ACTIVITY, request("Visita técnica", "Descrição suficiente para correção.", "2.00"), STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("submetida");
    }

    @Test
    void responsibleTeacherApprovesOrRejectsWithRequiredFields() {
        when(repository.find(ACTIVITY)).thenReturn(activity(ActivityStatus.SUBMITTED, null, null));
        when(repository.isResponsible(PROJECT, TEACHER)).thenReturn(true);
        ActivityDecisionRequest missingHours = new ActivityDecisionRequest(ActivityStatus.APPROVED, null, null);
        assertThatThrownBy(() -> service.decide(ACTIVITY, missingHours, TEACHER))
                .isInstanceOf(ConflictException.class).hasMessageContaining("horas");

        ActivityDecisionRequest rejection = new ActivityDecisionRequest(ActivityStatus.REJECTED, null, "Não há evidência suficiente.");
        when(repository.decide(ACTIVITY, rejection, TEACHER)).thenReturn(1);
        when(repository.find(ACTIVITY)).thenReturn(activity(ActivityStatus.SUBMITTED, null, null), activity(ActivityStatus.REJECTED, null, rejection.rejectionReason()));
        assertThat(service.decide(ACTIVITY, rejection, TEACHER).status()).isEqualTo(ActivityStatus.REJECTED);

        when(repository.isResponsible(PROJECT, STUDENT)).thenReturn(false);
        assertThatThrownBy(() -> service.decide(ACTIVITY, rejection, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("escopo");
    }

    @Test
    void approvedTotalUsesOnlyApprovedActivities() {
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.total(PROJECT, PERIOD, STUDENT)).thenReturn(new HoursTotalResponse(PROJECT, PERIOD, STUDENT, new BigDecimal("6.25")));
        assertThat(service.total(PROJECT, PERIOD, STUDENT).approvedHours()).isEqualByComparingTo("6.25");
    }

    private static ActivityRequest request(String title, String description, String hours) {
        return new ActivityRequest(title, description, new BigDecimal(hours));
    }

    private static ActivityResponse activity(ActivityStatus status, BigDecimal approved, String reason) {
        return new ActivityResponse(ACTIVITY, PROJECT, PERIOD, STUDENT, "Aluno teste", "aluno@example.com", "Visita técnica",
                "Relato detalhado da atividade realizada.", new BigDecimal("4.00"), approved, status, reason,
                NOW, status == ActivityStatus.SUBMITTED ? null : NOW, status == ActivityStatus.SUBMITTED ? null : TEACHER, NOW);
    }
}
