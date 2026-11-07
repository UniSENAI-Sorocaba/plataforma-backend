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

// TODO BE-007: validar papéis, campos e transições de status das propostas.
// não sei como mandar exceptions 403, então esta como prints por enquanto
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
        //AC-PI-001-01 — Criação: só quem pode criar é usuario com role de professor
        var RoleDoUsuario = Role.STUDENT; //nun tem header ainda para eu catar a role do usuario, então vai hardcoded
        if (RoleDoUsuario != Role.PROFESSOR) {
            //403
            System.out.println("erro de não professor, tentando criar projeto");
        }
        //AC-PI-001-03 — Responsável: não permite criar projeto com advisor que não seja professor
        var mano = userService.findEntity(request.advisorId());
        if (mano.getRole() != Role.PROFESSOR) {
            throw new ConflictException("erro de colocar advisor que não é professor");
        }
        copyToEntity(request, project);
        return mapper.toResponse(repository.saveAndFlush(project));
    }

    public PageResponse<ProjectResponse> findAll(Pageable pageable) {
        //AC-PI-001-02 — Publicação inicial: se não é autenticado, não acessa "rascunho"
        //AC-PI-001-08 — Publicação: findAll já não puxa dados dos participantes, isso é algo para quem for mexer com projectmembers
        //era para se usar dados do usario, mas vai com bool hardcoded (pra testar), já que não tem autenticação ainda
        var naoAutenticado = false;
        if (naoAutenticado) {
            return PageResponse.from(repository.findBystatusNot(ProjectStatus.PROPOSED ,pageable), mapper::toResponse);
        }
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ProjectResponse findById(UUID id) {
        //AC-PI-001-07 — Isolamento: impedir consulta de não autenticado a "rascunho" de projetos e não retornar dados do projeto no erro
        //era para se usar dados do usario, mas vai com bool hardcoded (pra testar), já que não tem autenticação ainda
        var naoAutenticado = true;
        var project = findEntity(id);
        if (naoAutenticado && project.getStatus() == ProjectStatus.PROPOSED) {
            throw new ResourceNotFoundException("projeto não encontrado");
        }
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        var project = findEntity(id);
        //AC-PI-001-07 — Isolamento: não retornar dados do projeto no erro de tentativa fora de escopo
        //AC-PI-001-05 — Edição: impedir não docente de fazer alteração de projeto
        //AC-PI-001-06 — Reabrir/arquivar: se tentar mudar de/para arquivamento, só permite se for docente
        //também ainda não é claro o que pode ou não ser alterado por docente e outros, (só as coias da spec-04 e spec-06)
        var RoleDoUsuario = Role.STUDENT; //nun tem header ainda para eu catar a role do usuario, então vai hardcoded
        if (RoleDoUsuario != Role.PROFESSOR) {
            //403
            System.out.println("erro de não professor, tentando alterar projeto");
        }
        //AC-PI-001-04 — Tipo e duração: não permite alteração de projectype, e talvez o outro campo imaginario (de continuo ou data final)
        if (request.projectType() != project.getProjectType()) {
            throw new ConflictException("erro de tentar alterar tipo do projeto");
        }
        //vamos fingir por enquanto que o outro campo imaginario é o course, e que não pode ser alterado também
        if (!request.course().equals(project.getCourse())) {
            throw new ConflictException("erro de tentar alterar continuo/data_final do projeto");
        }
        //AC-PI-001-03 — Responsável: não permite (alterar) advisor do projeto para algo que não seja professor
        var mano = userService.findEntity(request.advisorId());
        if (mano.getRole() != Role.PROFESSOR) {
            throw new ConflictException("erro de colocar advisor que não é professor");
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
