package br.senai.plataformadigital.academicpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import br.senai.plataformadigital.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

@ExtendWith(MockitoExtension.class)
class StudentAdministrationServiceTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    @Mock StudentAdministrationRepository repository;
    @Mock InstitutionalAuditRepository audit;
    private StudentAdministrationService service;

    @BeforeEach void setUp() { service = new StudentAdministrationService(repository, audit); }

    @Test void coordinatorCanListAndExportStudents() {
        when(repository.isCoordinator(ACTOR)).thenReturn(true);
        when(repository.findAll("ana")).thenReturn(List.of());
        when(repository.exportCsv("ana")).thenReturn("nome,email\n");

        assertThat(service.list(ACTOR, "ana")).isEmpty();
        assertThat(service.exportCsv(ACTOR, "ana")).contains("nome,email");
        verify(audit).record(ACTOR, "STUDENT_EXPORT", "student", null, "{}");
    }

    @Test void rejectsNonCoordinatorAndInvalidStatus() {
        when(repository.isCoordinator(ACTOR)).thenReturn(false);
        assertThatThrownBy(() -> service.list(ACTOR, "")).isInstanceOf(ConflictException.class);
        when(repository.isCoordinator(ACTOR)).thenReturn(true);
        assertThatThrownBy(() -> service.updateStatus(STUDENT, "pending", ACTOR)).isInstanceOf(ConflictException.class);
    }

    @Test void createsUpdatesAndChangesStatusWithAudit() {
        StudentRequest request = new StudentRequest("Ana", "ana@example.com", "12345678901", "A1", "T1");
        StudentResponse response = new StudentResponse(STUDENT, "Ana", request.email(), request.cpf(), "A1", "T1", "Turma", "2026.2", "active");
        when(repository.isCoordinator(ACTOR)).thenReturn(true);
        when(repository.create(request, ACTOR)).thenReturn(response);
        when(repository.update(STUDENT, new StudentUpdateRequest("Ana 2", request.email(), request.cpf(), "A2", "T1"))).thenReturn(response);

        assertThat(service.create(request, ACTOR)).isEqualTo(response);
        service.updateStatus(STUDENT, "active", ACTOR);
        assertThat(service.update(STUDENT, new StudentUpdateRequest("Ana 2", request.email(), request.cpf(), "A2", "T1"), ACTOR)).isEqualTo(response);
        verify(audit).record(ACTOR, "STUDENT_CREATED", "student", STUDENT, "{}");
        verify(audit).record(eq(ACTOR), eq("STUDENT_STATUS_CHANGED"), eq("student"), eq(STUDENT), anyString());
        verify(audit).record(ACTOR, "STUDENT_UPDATED", "student", STUDENT, "{}");
    }

    @Test void mapsCreateConflictsToDomainErrors() {
        StudentRequest request = new StudentRequest("Ana", "ana@example.com", "12345678901", "A1", "T1");
        when(repository.isCoordinator(ACTOR)).thenReturn(true);
        when(repository.create(request, ACTOR)).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.create(request, ACTOR)).isInstanceOf(ConflictException.class).hasMessageContaining("CPF");
        doThrow(new EmptyResultDataAccessException(1)).when(repository).create(request, ACTOR);
        assertThatThrownBy(() -> service.create(request, ACTOR)).isInstanceOf(ConflictException.class).hasMessageContaining("Turma");
    }

    @Test void importsHeaderBlankMalformedAndValidRows() {
        when(repository.isCoordinator(ACTOR)).thenReturn(true);
        StudentRequest valid = new StudentRequest("Ana", "ana@example.com", "12345678901", "A1", "T1");
        StudentResponse response = new StudentResponse(STUDENT, "Ana", valid.email(), valid.cpf(), "A1", "T1", "Turma", "2026.2", "pending");
        when(repository.create(any(StudentRequest.class), eq(ACTOR))).thenReturn(response);
        StudentImportResponse result = service.importCsv("nome,email,cpf,matricula,turma\n\nruim\nAna,ana@example.com,123.456.789-01,A1,T1".getBytes(), ACTOR);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.rejected()).isEqualTo(1);
    }
}
