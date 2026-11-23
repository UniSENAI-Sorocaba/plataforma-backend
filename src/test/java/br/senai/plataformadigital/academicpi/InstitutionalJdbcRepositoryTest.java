package br.senai.plataformadigital.academicpi;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class InstitutionalJdbcRepositoryTest {
    @Mock JdbcTemplate jdbc;

    @Test
    void studentRepositoryChecksGlobalCoordinatorRole() {
        UUID actor = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(actor))).thenReturn(Boolean.TRUE);

        assertTrue(new StudentAdministrationRepository(jdbc).isCoordinator(actor));
        verify(jdbc).queryForObject(anyString(), eq(Boolean.class), eq(actor));
    }

    @Test
    void studentRepositoryUpdatesUserAndAcademicLinkStatus() {
        UUID student = UUID.randomUUID();
        when(jdbc.update(anyString(), eq("inactive"), eq(student))).thenReturn(1);

        new StudentAdministrationRepository(jdbc).updateStatus(student, "inactive");

        verify(jdbc, org.mockito.Mockito.times(2)).update(anyString(), eq("inactive"), eq(student));
    }

    @Test
    void academicClassRepositoryUpdatesStatus() {
        UUID classId = UUID.randomUUID();
        when(jdbc.update(anyString(), eq(false), eq(classId))).thenReturn(1);

        new AcademicClassRepository(jdbc).updateStatus(classId, false);

        verify(jdbc).update(anyString(), eq(false), eq(classId));
    }
}
