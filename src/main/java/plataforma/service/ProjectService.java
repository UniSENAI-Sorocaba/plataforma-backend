package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.ProjectRequest;
import plataforma.dto.ProjectResponse;
import plataforma.exception.PropertyReferenceException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ProjectMapper;
import plataforma.model.Project;
import plataforma.model.Role;
import plataforma.repository.ProjectRepository;

// TODO BE-007: validar papéis, campos e transições de status das propostas.
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final UserService userService;

    public ProjectService(ProjectRepository repository, ProjectMapper mapper, UserService userService) {
        this.repository = repository;
        this.mapper = mapper;
        this.userService = userService;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        var project = new Project();
        var mano = userService.findEntity(request.advisorId());
        //AC-PI-001-03 — Responsável
        if (mano.getRole() != Role.PROFESSOR) {
            System.out.println("finge que aqui tem um erro");
        }
        copyToEntity(request, project);
        return mapper.toResponse(repository.saveAndFlush(project));
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
        //AC-PI-001-04 — Tipo e duração
        if (request.projectType() != project.getProjectType()) {
            System.out.println("deu pobrema ó (ou melhor, ERA para dar problema)");
            //throw new PropertyReferenceException("Tipo do projeto não deve ser alterado " + request.projectType() + " != " + project.getProjectType());
        }
        copyToEntity(request, project);
        return mapper.toResponse(repository.saveAndFlush(project));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public Project findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + id));
    }

    private void copyToEntity(ProjectRequest request, Project project) {
        mapper.copyToEntity(request, project);
        project.setLeader(userService.findEntity(request.leaderId()));
        project.setAdvisor(request.advisorId() == null ? null : userService.findEntity(request.advisorId()));
    }
}
