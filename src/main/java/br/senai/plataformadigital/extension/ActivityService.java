package br.senai.plataformadigital.extension;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class ActivityService {
    private final ActivityRepository repository;
    private final Clock clock;

    public ActivityService(ActivityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ActivityResponse submit(UUID projectId, UUID periodId, ActivityRequest request, UUID actor) {
        requireParticipant(projectId, periodId, actor);
        requireBeforeDeadline(projectId, periodId);
        validateHours(request.requestedHours());
        try { return repository.find(repository.insert(projectId, periodId, actor, request)); }
        catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Já existe uma atividade com este título neste período.");
        }
    }

    public List<ActivityResponse> mine(UUID projectId, UUID periodId, UUID actor) {
        requireParticipant(projectId, periodId, actor);
        return repository.findMine(projectId, periodId, actor);
    }

    public List<ActivityResponse> project(UUID projectId, UUID periodId, UUID actor) {
        requireResponsible(projectId, actor);
        return repository.findProject(projectId, periodId);
    }

    public HoursTotalResponse total(UUID projectId, UUID periodId, UUID actor) {
        requireParticipant(projectId, periodId, actor);
        return repository.total(projectId, periodId, actor);
    }

    @Transactional
    public ActivityResponse correct(UUID id, ActivityRequest request, UUID actor) {
        ActivityResponse activity = get(id);
        if (!activity.studentId().equals(actor)) throw new ConflictException("Atividade fora do escopo do aluno.");
        requireParticipant(activity.projectId(), activity.periodId(), actor);
        requireBeforeDeadline(activity.projectId(), activity.periodId());
        validateHours(request.requestedHours());
        try {
            if (repository.updateSubmission(id, actor, request) != 1) {
                throw new ConflictException("Somente atividade submetida ou recusada pode ser corrigida.");
            }
            return get(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Já existe uma atividade com este título neste período.");
        }
    }

    @Transactional
    public ActivityResponse decide(UUID id, ActivityDecisionRequest request, UUID actor) {
        ActivityResponse activity = get(id);
        requireResponsible(activity.projectId(), actor);
        validateDecision(request);
        if (repository.decide(id, request, actor) != 1) throw new ConflictException("Atividade não encontrada.");
        return get(id);
    }

    public ActivityResponse get(UUID id) {
        try { return repository.find(id); }
        catch (EmptyResultDataAccessException missing) { throw new ResourceNotFoundException("Atividade não encontrada."); }
    }

    private void validateDecision(ActivityDecisionRequest request) {
        if (request.status() == ActivityStatus.APPROVED) {
            if (request.approvedHours() == null) throw new ConflictException("A aprovação exige horas aprovadas.");
            validateHours(request.approvedHours());
            if (request.rejectionReason() != null && !request.rejectionReason().isBlank()) {
                throw new ConflictException("A aprovação não aceita motivo de recusa.");
            }
        } else if (request.status() == ActivityStatus.REJECTED) {
            if (request.approvedHours() != null) throw new ConflictException("A recusa não aceita horas aprovadas.");
            if (request.rejectionReason() == null || request.rejectionReason().trim().length() < 3) {
                throw new ConflictException("A recusa exige um motivo.");
            }
        } else if (request.approvedHours() != null || request.rejectionReason() != null) {
            throw new ConflictException("Uma atividade submetida não possui decisão.");
        }
    }

    private void validateHours(BigDecimal hours) {
        if (hours == null || hours.compareTo(new BigDecimal("0.25")) < 0
                || hours.compareTo(new BigDecimal("100.00")) > 0
                || hours.remainder(new BigDecimal("0.25")).signum() != 0) {
            throw new ConflictException("As horas devem estar entre 0,25 e 100, em incrementos de 0,25.");
        }
    }

    private void requireBeforeDeadline(UUID projectId, UUID periodId) {
        Instant deadline = repository.periodEndsAt(projectId, periodId);
        if (!clock.instant().isBefore(deadline)) throw new ConflictException("O prazo do período já foi encerrado.");
    }

    private void requireParticipant(UUID projectId, UUID periodId, UUID actor) {
        if (!repository.isActiveParticipant(projectId, periodId, actor)) {
            throw new ConflictException("Somente participante ativo pode enviar atividades.");
        }
    }

    private void requireResponsible(UUID projectId, UUID actor) {
        if (!repository.isResponsible(projectId, actor) && !repository.isCoordinator(actor)) throw new ConflictException("Docente fora do escopo do projeto.");
    }
}
