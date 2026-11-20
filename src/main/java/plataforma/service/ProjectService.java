package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.ProjectRequest;
import plataforma.dto.ProjectResponse;
import plataforma.exception.ConflictException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ProjectMapper;
import plataforma.model.Project;
import plataforma.model.ProjectStatus;
import plataforma.model.Role;
import plataforma.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;

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
        var userRole = Role.PROFESSOR; //não tem header ainda para se pegar o role do usuário.
        if (userRole != Role.PROFESSOR) {
            throw new AccessDeniedException("Erro de não professor, tentando criar projeto");
        }
        var advisor = userService.findEntity(request.advisorId());
        if (advisor.getRole() != Role.PROFESSOR) {
            throw new ConflictException("erro de colocar advisor que não é professor");
        }
        copyToEntity(request, project);
        return mapper.toResponse(repository.saveAndFlush(project));
    }

    public PageResponse<ProjectResponse> findAll(Pageable pageable) {
        var naoAutenticado = false; //era para checar que se o usuário está autenticado, mas não tem autenticação ainda.
        if (naoAutenticado) {
            return PageResponse.from(repository.findByStatus(ProjectStatus.APPROVED ,pageable), mapper::toResponse);
        }
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ProjectResponse findById(UUID id) {
        var naoAutenticado = false; //era para checar que se o usuário está autenticado, mas não tem autenticação ainda.
        var project = findEntity(id);
        if (naoAutenticado && project.getStatus() != ProjectStatus.APPROVED) {
            throw new ResourceNotFoundException("projeto não encontrado");
        }
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        var project = findEntity(id);
        var userRole = Role.PROFESSOR; //não tem header ainda para se pegar o role do usuário.
        if (userRole != Role.PROFESSOR) {
            throw new AccessDeniedException("Erro de não professor, tentando alterar projeto");
        }
        if (request.projectType() != project.getProjectType()) {
            throw new ConflictException("erro de tentar alterar tipo do projeto");
        }
        var advisor = userService.findEntity(request.advisorId());
        if (advisor.getRole() != Role.PROFESSOR) {
            throw new ConflictException("erro de colocar advisor que não é professor");
        }
        copyToEntity(request, project);
        return mapper.toResponse(repository.saveAndFlush(project));
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
