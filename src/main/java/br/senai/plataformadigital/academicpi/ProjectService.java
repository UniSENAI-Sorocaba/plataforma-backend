package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class ProjectService {
    private final ProjectRepository repository;
    private final Clock clock;

    public ProjectService(ProjectRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, UUID actor) {
        requireTeacher(actor);
        validateProject(request);
        if (request.responsibleTeacherIds().stream().distinct().count() != request.responsibleTeacherIds().size()) {
            throw new ConflictException("Docentes responsáveis duplicados.");
        }
        request.responsibleTeacherIds().forEach(this::requireTeacher);
        if (!request.responsibleTeacherIds().contains(actor)) {
            throw new ConflictException("O docente criador deve ser responsável pelo projeto.");
        }
        UUID id = repository.insertProject(request, actor);
        return get(id);
    }

    public ProjectResponse get(UUID id) {
        try {
            return repository.find(id);
        } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
            throw new ResourceNotFoundException("Projeto não encontrado.");
        }
    }

    public List<ProjectResponse> publicProjects() {
        return repository.findPublic();
    }

    public List<ProjectResponse> managedProjects(UUID actor) {
        requireTeacher(actor);
        return repository.findManaged(actor);
    }

    public List<EnrollmentPeriodResponse> periods(UUID projectId, UUID actor) {
        requireResponsible(projectId, actor);
        return repository.findPeriods(projectId);
    }

    public List<ParticipationRequestResponse> projectRequests(UUID projectId, UUID actor) {
        requireResponsible(projectId, actor);
        return repository.findProjectRequests(projectId);
    }

    public List<ParticipationRequestResponse> ownRequests(UUID actor) {
        requireStudent(actor);
        return repository.findStudentRequests(actor);
    }

    public List<ParticipantResponse> participants(UUID projectId, UUID periodId, UUID actor) {
        requireResponsible(projectId, actor);
        return repository.findParticipants(projectId, periodId);
    }

    @Transactional
    public EnrollmentPeriodResponse createPeriod(UUID projectId, EnrollmentPeriodRequest request, UUID actor) {
        if (!repository.hasRole(actor, "COORDENACAO")) {
            throw new ConflictException("A criação de períodos exige papel de coordenação.");
        }
        if (request.startsAt().compareTo(request.endsAt()) >= 0) {
            throw new ConflictException("O início do período deve ser anterior ao fim.");
        }
        try {
            return repository.insertPeriod(projectId, request, actor);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Já existe um período para este projeto e termo, ou o projeto não existe.");
        }
    }

    @Transactional
    public ProjectResponse changePublication(UUID id, ProjectPublicationStatus status, UUID actor) {
        requireResponsible(id, actor);
        try {
            repository.updatePublication(id, status);
            return get(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Publicação inválida para os campos atuais.");
        }
    }

    @Transactional
    public ProjectResponse changeLifecycle(UUID id, ProjectLifecycleStatus status, UUID actor) {
        requireResponsible(id, actor);
        try {
            repository.updateLifecycle(id, status);
            return get(id);
        } catch (IllegalStateException exception) {
            throw new ResourceNotFoundException("Projeto não encontrado.");
        }
    }

    @Transactional
    public ParticipationRequestResponse requestParticipation(UUID projectId, UUID actor) {
        requireStudent(actor);
        UUID period = repository.activePeriod(projectId, clock.instant());
        if (period == null) throw new ConflictException("Não há período de inscrição vigente.");
        try {
            return repository.createRequest(projectId, period, actor);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Já existe uma solicitação para este projeto e período.");
        }
    }

    @Transactional
    public ParticipationRequestResponse decide(UUID requestId, ParticipationRequestStatus status, UUID actor) {
        ParticipationRequestResponse request = repository.findRequest(requestId);
        requireResponsible(request.projectId(), actor);
        try {
            return repository.decideRequest(requestId, status, actor);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A aprovação excede o limite de participação ou a solicitação já foi decidida.");
        }
    }

    @Transactional
    public ParticipationRequestResponse withdraw(UUID requestId, UUID actor) {
        requireStudent(actor);
        ParticipationRequestResponse request;
        try {
            request = repository.findRequest(requestId);
        } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
            throw new ResourceNotFoundException("Solicitação não encontrada.");
        }
        UUID activePeriod = repository.activePeriod(request.projectId(), clock.instant());
        if (!request.studentId().equals(actor) || !request.periodId().equals(activePeriod)) {
            throw new ConflictException("A desistência só é permitida pelo aluno durante o período vigente.");
        }
        if (repository.withdrawRequest(requestId, actor, activePeriod) != 1) {
            throw new ConflictException("A solicitação não está pendente.");
        }
        return repository.findRequest(requestId);
    }

    @Transactional
    public void remove(UUID projectId, UUID periodId, UUID studentId, String reason, UUID actor) {
        requireResponsible(projectId, actor);
        if (reason == null || reason.isBlank()) throw new ConflictException("A justificativa da remoção é obrigatória.");
        if (repository.removeParticipant(projectId, periodId, studentId, actor, reason.trim()) != 1) {
            throw new ResourceNotFoundException("Participante ativo não encontrado.");
        }
    }

    private void validateProject(ProjectRequest request) {
        if (request.title() == null || request.title().trim().length() < 5) throw new ConflictException("Título inválido.");
        if (request.continuity() == ProjectContinuity.FIXED_END &&
                (request.endDate() == null || request.endDate().isBefore(java.time.LocalDate.now(clock)))) {
            throw new ConflictException("A data final deve ser futura.");
        }
        if (request.publicationStatus() == ProjectPublicationStatus.PUBLISHED &&
                (request.summary() == null || request.description() == null)) {
            throw new ConflictException("Publicação exige resumo e descrição.");
        }
    }

    private void requireTeacher(UUID userId) {
        if (!repository.hasRole(userId, "DOCENTE") && !repository.hasRole(userId, "COORDENACAO")) {
            throw new ConflictException("Usuário não possui papel docente.");
        }
    }

    private void requireStudent(UUID userId) {
        if (!repository.isActiveStudent(userId)) throw new ConflictException("Aluno inelegível.");
    }

    private void requireResponsible(UUID projectId, UUID actor) {
        if (!repository.isResponsible(projectId, actor)) throw new ConflictException("Docente fora do escopo do projeto.");
    }
}
