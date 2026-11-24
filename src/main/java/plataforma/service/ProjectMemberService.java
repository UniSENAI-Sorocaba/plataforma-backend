package plataforma.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.ProjectMemberRequest;
import plataforma.dto.ProjectMemberResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ProjectMemberMapper;
import plataforma.model.InvitationStatus;
import plataforma.model.ProjectMember;
import plataforma.model.Role;
import plataforma.repository.ProjectMemberRepository;
import org.springframework.security.access.AccessDeniedException;

// TODO BE-008: convites aceitam/recusam sem duplicidade.
@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository repository;
    private final ProjectMemberMapper mapper;
    private final ProjectService projectService;
    private final UserService userService;

    public ProjectMemberService(ProjectMemberRepository repository, ProjectMemberMapper mapper,
            ProjectService projectService, UserService userService) {
        this.repository = repository;
        this.mapper = mapper;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Transactional
    public ProjectMemberResponse create(ProjectMemberRequest request) {
        var member = new ProjectMember();
        copyToEntity(request, member);
        return mapper.toResponse(repository.saveAndFlush(member));
    }

    public PageResponse<ProjectMemberResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ProjectMemberResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public ProjectMemberResponse update(UUID id, ProjectMemberRequest request) {
        
        var member = findEntity(id);
        
        if (member.getStatus() != request.status()) {
            //AC-PI-001-10 — Decisão docente: apenas professor aprova ou nega solicitação
            var userRole = Role.PROFESSOR; //não tem header ainda para se pegar o role do usuário.
            if (userRole != Role.PROFESSOR) {
                throw new AccessDeniedException("Erro de não professor, tentando alterar situação aluno/projeto");
            }
            //AC-PI-001-12 — Concorrência: não prosseguir caso a aprovação passe do limite de PE/PI do aluno
            if (request.status() == InvitationStatus.ACCEPTED) {
                List<ProjectMember> lista = repository.findByUserAndStatus(member.getUser().getId(), InvitationStatus.ACCEPTED);
            }
        }
        copyToEntity(request, member);
        return mapper.toResponse(repository.saveAndFlush(member));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public ProjectMember findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro de projeto não encontrado: " + id));
    }

    private void copyToEntity(ProjectMemberRequest request, ProjectMember member) {
        mapper.copyToEntity(request, member);
        member.setProject(projectService.findEntity(request.projectId()));
        member.setUser(userService.findEntity(request.userId()));
    }
}
