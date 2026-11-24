package br.senai.plataformadigital.extension;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
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
public class ExtensionService {
    private final ExtensionRepository repository;
    private final Clock clock;

    public ExtensionService(ExtensionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public OpportunityResponse create(OpportunityRequest request, UUID actor) {
        requireCoordinator(actor);
        validateWindow(request.startsAt(), request.endsAt());
        try {
            return repository.findOpportunity(repository.insertOpportunity(request, actor));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("O projeto associado ao edital não existe.");
        }
    }

    @Transactional
    public OpportunityResponse update(UUID id, OpportunityRequest request, UUID actor) {
        requireCoordinator(actor);
        validateWindow(request.startsAt(), request.endsAt());
        if (repository.updateOpportunity(id, request) != 1) throw new ConflictException("Somente editais em rascunho podem ser editados.");
        return get(id);
    }

    @Transactional
    public void delete(UUID id, UUID actor) {
        requireCoordinator(actor);
        if (repository.deleteOpportunity(id) != 1) throw new ConflictException("Somente editais em rascunho podem ser excluídos.");
    }

    public List<OpportunityResponse> publicOpportunities() { return repository.findPublicOpportunities(); }

    public List<OpportunityResponse> allOpportunities(UUID actor) {
        requireCoordinator(actor);
        return repository.findAllOpportunities();
    }

    public OpportunityResponse get(UUID id) {
        try { return repository.findOpportunity(id); }
        catch (EmptyResultDataAccessException missing) { throw new ResourceNotFoundException("Edital não encontrado."); }
    }

    public OpportunityResponse publicOpportunity(UUID id) {
        OpportunityResponse opportunity = get(id);
        if (opportunity.status() == OpportunityStatus.DRAFT) throw new ResourceNotFoundException("Edital não encontrado.");
        return opportunity;
    }

    @Transactional
    public OpportunityResponse publish(UUID id, UUID actor) {
        requireCoordinator(actor);
        changeStatus(id, OpportunityStatus.DRAFT, OpportunityStatus.PUBLISHED);
        return get(id);
    }

    @Transactional
    public OpportunityResponse close(UUID id, UUID actor) {
        requireCoordinator(actor);
        changeStatus(id, OpportunityStatus.PUBLISHED, OpportunityStatus.CLOSED);
        return get(id);
    }

    @Transactional
    public ApplicationResponse apply(UUID opportunityId, UUID actor) {
        requireStudent(actor);
        OpportunityResponse opportunity = get(opportunityId);
        requireOpen(opportunity);
        try { return repository.insertApplication(opportunityId, actor); }
        catch (DataIntegrityViolationException exception) {
            throw new ConflictException("O aluno já possui uma inscrição neste edital.");
        }
    }

    public List<ApplicationResponse> ownApplications(UUID actor) {
        requireStudent(actor);
        return repository.findStudentApplications(actor);
    }

    public List<ApplicationResponse> opportunityApplications(UUID opportunityId) {
        get(opportunityId);
        return repository.findOpportunityApplications(opportunityId);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId, UUID actor) {
        requireStudent(actor);
        ApplicationResponse application;
        try { application = repository.findApplication(applicationId); }
        catch (EmptyResultDataAccessException missing) { throw new ResourceNotFoundException("Inscrição não encontrada."); }
        if (!application.studentId().equals(actor)) throw new ConflictException("Inscrição fora do escopo do aluno.");
        requireOpen(get(application.opportunityId()));
        if (repository.withdrawApplication(applicationId, actor, new ExtensionRepository.InstantHolder(clock.instant())) != 1) {
            throw new ConflictException("A inscrição já foi retirada ou não está ativa.");
        }
        return repository.findApplication(applicationId);
    }

    private void changeStatus(UUID id, OpportunityStatus from, OpportunityStatus to) {
        if (repository.updateStatus(id, from, to, new ExtensionRepository.InstantHolder(clock.instant())) != 1) {
            throw new ConflictException("Transição de status do edital inválida.");
        }
    }

    private void requireOpen(OpportunityResponse opportunity) {
        Instant now = clock.instant();
        if (opportunity.status() != OpportunityStatus.PUBLISHED
                || now.isBefore(opportunity.startsAt()) || !now.isBefore(opportunity.endsAt())) {
            throw new ConflictException("O edital não está dentro da janela de inscrição.");
        }
    }

    private void validateWindow(Instant startsAt, Instant endsAt) {
        if (!startsAt.isBefore(endsAt)) throw new ConflictException("A janela do edital é inválida.");
    }

    private void requireCoordinator(UUID actor) {
        if (!repository.hasRole(actor, "COORDENACAO")) throw new ConflictException("A operação exige papel de coordenação.");
    }

    private void requireStudent(UUID actor) {
        if (!repository.isActiveStudent(actor)) throw new ConflictException("Aluno inelegível.");
    }
}
