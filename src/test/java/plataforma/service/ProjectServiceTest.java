package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plataforma.dto.ProjectRequest;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.exception.ProjectRuleViolationException;
import plataforma.mapper.ProjectMapper;
import plataforma.model.AuditOperation;
import plataforma.model.Project;
import plataforma.model.ProjectStatus;
import plataforma.model.ProjectType;
import plataforma.model.Role;
import plataforma.model.UserAccount;
import plataforma.repository.ProjectRepository;

class ProjectServiceTest {

    private ProjectRepository repository;
    private UserService userService;
    private AuditEventService auditEventService;
    private ProjectService service;
    private UUID leaderId;
    private UUID advisorId;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectRepository.class);
        userService = mock(UserService.class);
        auditEventService = mock(AuditEventService.class);
        service = new ProjectService(repository, new ProjectMapper(), userService, auditEventService);
        leaderId = UUID.randomUUID();
        advisorId = UUID.randomUUID();

        var leader = userWithRole(leaderId, Role.STUDENT);
        var advisor = userWithRole(advisorId, Role.PROFESSOR);
        when(userService.findEntity(leaderId)).thenReturn(leader);
        when(userService.findEntity(advisorId)).thenReturn(advisor);
        when(repository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void criaPropostaComoRascunhoComEstudanteELiderDocente() {
        var response = service.create(request(ProjectStatus.PROPOSED));

        assertThat(response.status()).isEqualTo(ProjectStatus.PROPOSED);
        assertThat(response.leaderId()).isEqualTo(leaderId);
        assertThat(response.advisorId()).isEqualTo(advisorId);

        var auditRequest = ArgumentCaptor.forClass(RecordAuditEventRequest.class);
        verify(auditEventService).record(auditRequest.capture());
        assertThat(auditRequest.getValue().operation()).isEqualTo(AuditOperation.INSERT);
        assertThat(auditRequest.getValue().actorId()).isNull();
        assertThat(auditRequest.getValue().newData()).containsEntry("status", "PROPOSED");
    }

    @Test
    void rejeitaCriacaoEmEstadoDiferenteDeRascunho() {
        assertThatThrownBy(() -> service.create(request(ProjectStatus.IN_PROGRESS)))
                .isInstanceOf(ProjectRuleViolationException.class)
                .hasMessageContaining("rascunho");

        verify(repository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void rejeitaLiderQueNaoSejaEstudante() {
        when(userService.findEntity(leaderId)).thenReturn(userWithRole(leaderId, Role.PROFESSOR));

        assertThatThrownBy(() -> service.create(request(ProjectStatus.PROPOSED)))
                .isInstanceOf(ProjectRuleViolationException.class)
                .hasMessageContaining("líder");

        verify(repository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void rejeitaResponsavelQueNaoSejaDocente() {
        when(userService.findEntity(advisorId)).thenReturn(userWithRole(advisorId, Role.STUDENT));

        assertThatThrownBy(() -> service.create(request(ProjectStatus.PROPOSED)))
                .isInstanceOf(ProjectRuleViolationException.class)
                .hasMessageContaining("docente");

        verify(repository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void rejeitaTransicaoDeEstadoAindaNaoDefinida() {
        var projectId = UUID.randomUUID();
        var existing = new Project();
        existing.setStatus(ProjectStatus.PROPOSED);
        when(repository.findById(projectId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(projectId, request(ProjectStatus.APPROVED)))
                .isInstanceOf(ProjectRuleViolationException.class)
                .hasMessageContaining("transição");

        verify(repository, never()).saveAndFlush(any(Project.class));
        verify(auditEventService, never()).record(any(RecordAuditEventRequest.class));
        }

        @Test
        void auditaEdicaoEExclusaoComDadosMinimos() {
        var projectId = UUID.randomUUID();
        var existing = new Project();
        existing.setProjectType(ProjectType.ACADEMIC_PI);
        existing.setStatus(ProjectStatus.PROPOSED);
        when(repository.findById(projectId)).thenReturn(Optional.of(existing));

        service.update(projectId, request(ProjectStatus.PROPOSED));
        service.delete(projectId);

        var auditRequests = ArgumentCaptor.forClass(RecordAuditEventRequest.class);
        verify(auditEventService, org.mockito.Mockito.times(2)).record(auditRequests.capture());
        assertThat(auditRequests.getAllValues()).extracting(RecordAuditEventRequest::operation)
            .containsExactly(AuditOperation.UPDATE, AuditOperation.DELETE);
        assertThat(auditRequests.getAllValues()).allSatisfy(auditRequest -> {
            assertThat(auditRequest.schemaName()).isEqualTo("academic_pi");
            assertThat(auditRequest.tableName()).isEqualTo("project");
            assertThat(auditRequest.actorId()).isNull();
        });
    }

    private ProjectRequest request(ProjectStatus status) {
        return new ProjectRequest("Projeto", "Descrição", ProjectType.ACADEMIC_PI, status,
            "2026.2", "Engenharia", leaderId, advisorId);
    }

    private static UserAccount userWithRole(UUID id, Role role) {
        var user = mock(UserAccount.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }
}