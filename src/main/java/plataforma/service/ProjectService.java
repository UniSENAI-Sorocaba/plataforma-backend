package plataforma.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.ProjectRequest;
import plataforma.dto.ProjectResponse;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.exception.ResourceNotFoundException;
import plataforma.exception.ProjectRuleViolationException;
import plataforma.mapper.ProjectMapper;
import plataforma.model.AuditOperation;
import plataforma.model.Project;
import plataforma.model.ProjectStatus;
import plataforma.model.Role;
import plataforma.repository.ProjectRepository;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final UserService userService;
    private final AuditEventService auditEventService;

    public ProjectService(ProjectRepository repository, ProjectMapper mapper, UserService userService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.mapper = mapper;
        this.userService = userService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (request.status() != ProjectStatus.PROPOSED) {
            throw new ProjectRuleViolationException("Uma proposta nova deve iniciar como rascunho (PROPOSED).");
        }
        var project = new Project();
        copyToEntity(request, project);
        project.setStatus(ProjectStatus.PROPOSED);
        var saved = repository.saveAndFlush(project);
        recordAudit(AuditOperation.INSERT, saved, null, snapshot(saved));
        return mapper.toResponse(saved);
    }

    public PageResponse<ProjectResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ProjectResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        var project = findEntity(id);
        if (request.status() != project.getStatus()) {
            throw new ProjectRuleViolationException("A transição de estado solicitada não está definida.");
        }
        var oldData = snapshot(project);
        copyToEntity(request, project);
        var saved = repository.saveAndFlush(project);
        recordAudit(AuditOperation.UPDATE, saved, oldData, snapshot(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        var project = findEntity(id);
        var oldData = snapshot(project);
        repository.delete(project);
        recordAudit(AuditOperation.DELETE, project, oldData, null);
    }

    public Project findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + id));
    }

    private void copyToEntity(ProjectRequest request, Project project) {
        if (request.advisorId() == null) {
            throw new ProjectRuleViolationException("A proposta exige um docente responsável.");
        }

        var leader = userService.findEntity(request.leaderId());
        var advisor = userService.findEntity(request.advisorId());
        if (leader.getRole() != Role.STUDENT) {
            throw new ProjectRuleViolationException("O líder da proposta deve ser um estudante.");
        }
        if (advisor.getRole() != Role.PROFESSOR) {
            throw new ProjectRuleViolationException("O responsável pela proposta deve ser um docente.");
        }

        mapper.copyToEntity(request, project);
        project.setLeader(leader);
        project.setAdvisor(advisor);
    }

    private void recordAudit(AuditOperation operation, Project project,
            Map<String, Object> oldData, Map<String, Object> newData) {
        auditEventService.record(new RecordAuditEventRequest(
                "academic_pi", "project", operation, project.getId(), null, oldData, newData));
    }

    private static Map<String, Object> snapshot(Project project) {
        return Map.of("projectType", project.getProjectType().name(), "status", project.getStatus().name());
    }
}
