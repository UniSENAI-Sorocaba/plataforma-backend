package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.ProjectMemberRequest;
import plataforma.dto.ProjectMemberResponse;
import plataforma.exception.ConflictException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ProjectMemberMapper;
import plataforma.model.InvitationStatus;
import plataforma.model.ProjectMember;
import plataforma.model.ProjectType;
import plataforma.model.Role;
import plataforma.repository.ProjectMemberRepository;
import plataforma.repository.UserAccountRepository;

// TODO BE-008: convites aceitam/recusam sem duplicidade.
@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    /** AC-PI-001-09: o período de inscrição não está no modelo; fica fixo até existir requisito. */
    static final boolean PERIODO_INSCRICAO_VIGENTE = true;

    /** AC-PI-001-10 e AC-PI-001-13: sem identidade; o docente da decisão fica fixo como professor do projeto. */
    static final boolean DOCENTE_AUTORIZADO_NO_PROJETO = true;

    private final ProjectMemberRepository repository;
    private final ProjectMemberMapper mapper;
    private final ProjectService projectService;
    private final UserService userService;
    private final UserAccountRepository userRepository;

    public ProjectMemberService(ProjectMemberRepository repository, ProjectMemberMapper mapper,
            ProjectService projectService, UserService userService, UserAccountRepository userRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.projectService = projectService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectMemberResponse create(ProjectMemberRequest request) {
        // AC-PI-001-09 — sem identidade autenticada; o solicitante fica fixo como aluno.
        var papelDoSolicitante = Role.STUDENT;
        exigirSolicitacaoDeAluno(papelDoSolicitante, PERIODO_INSCRICAO_VIGENTE, request.status());
        var member = new ProjectMember();
        copyToEntity(request, member);
        return mapper.toResponse(repository.saveAndFlush(member));
    }

    /** Solicitação do aluno: papel aluno, período vigente e status pendente (INVITED). */
    static void exigirSolicitacaoDeAluno(Role papelDoSolicitante, boolean periodoVigente, InvitationStatus status) {
        if (papelDoSolicitante != Role.STUDENT) {
            throw new AccessDeniedException("Somente aluno solicita participação");
        }
        if (!periodoVigente) {
            throw new ConflictException("Solicitação fora do período de inscrição vigente");
        }
        if (status != InvitationStatus.INVITED) {
            throw new ConflictException("A solicitação do aluno entra pendente");
        }
    }

    public PageResponse<ProjectMemberResponse> findAll(Pageable pageable) {
        // AC-PI-001-15: sem autenticação, a listagem é pública e omite solicitação pendente.
        // A resposta não leva justificativa de remoção nem outro agregado de membros.
        return PageResponse.from(repository.findByStatusNot(InvitationStatus.INVITED, pageable), mapper::toResponse);
    }

    public ProjectMemberResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public ProjectMemberResponse update(UUID id, ProjectMemberRequest request) {
        var member = findEntity(id);
        if (!member.getProject().getId().equals(request.projectId())
                || !member.getUser().getId().equals(request.userId())) {
            throw new AccessDeniedException("A decisão não altera participante de outro projeto");
        }
        if (member.getStatus() != request.status()) {
            // AC-PI-001-10 — papel e vínculo do docente estão fixos; não há ator autenticado.
            exigirDecisaoDocente(Role.PROFESSOR, DOCENTE_AUTORIZADO_NO_PROJETO);
            if (request.status() == InvitationStatus.ACCEPTED) {
                exigirLimiteNaAprovacao(member);
            }
        }
        copyToEntity(request, member);
        return mapper.toResponse(repository.saveAndFlush(member));
    }

    /** AC-PI-001-10: aluno ou docente fora do projeto não decide. */
    static void exigirDecisaoDocente(Role papel, boolean autorizadoNoProjeto) {
        if (papel != Role.PROFESSOR || !autorizadoNoProjeto) {
            throw new AccessDeniedException("Docente sem autorização neste projeto");
        }
    }

    /**
     * AC-PI-001-11 e AC-PI-001-12. PI e Extensão são limites separados.
     * Híbrido consome os dois na mesma aprovação. O lock no aluno impede duas transações
     * de gravarem o segundo vínculo.
     */
    private void exigirLimiteNaAprovacao(ProjectMember member) {
        var alunoId = member.getUser().getId();
        userRepository.findByIdForUpdate(alunoId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + alunoId));
        var aceitos = repository.findByUserAndStatus(member.getUser(), InvitationStatus.ACCEPTED);
        var piOcupado = false;
        var extensaoOcupada = false;
        for (ProjectMember outro : aceitos) {
            if (outro.getId().equals(member.getId())) {
                continue;
            }
            var tipo = outro.getProject().getProjectType();
            if (tipo == ProjectType.ACADEMIC_PI || tipo == ProjectType.HYBRID) {
                piOcupado = true;
            }
            if (tipo == ProjectType.EXTENSION || tipo == ProjectType.HYBRID) {
                extensaoOcupada = true;
            }
        }
        var tipoNovo = member.getProject().getProjectType();
        var consomePi = tipoNovo == ProjectType.ACADEMIC_PI || tipoNovo == ProjectType.HYBRID;
        var consomeExtensao = tipoNovo == ProjectType.EXTENSION || tipoNovo == ProjectType.HYBRID;
        if ((consomePi && piOcupado) || (consomeExtensao && extensaoOcupada)) {
            throw new ConflictException("Aluno já vinculado nesse limite de participação");
        }
    }

    @Transactional
    public void delete(UUID id, String justification) {
        if (justification == null || justification.isBlank()) {
            throw new ConflictException("Remoção exige justificativa");
        }
        // AC-PI-001-13 — aluno e docente fora do projeto não removem. Papel fixo, sem ator autenticado.
        exigirDecisaoDocente(Role.PROFESSOR, DOCENTE_AUTORIZADO_NO_PROJETO);
        var member = findEntity(id);
        userRepository.findByIdForUpdate(member.getUser().getId());
        repository.delete(member);
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
