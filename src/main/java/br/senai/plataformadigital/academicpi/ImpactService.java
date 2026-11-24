package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class ImpactService {
    private final ImpactRepository repository;
    public ImpactService(ImpactRepository repository) { this.repository = repository; }
    public ImpactResponse create(UUID project, ImpactRequest request, UUID actor) { requireResponsible(project, actor); return repository.insert(project, request, actor); }
    public ImpactResponse submit(UUID id, UUID actor) { if (!repository.canManage(id, actor)) throw new ConflictException("Docente fora do escopo do impacto."); return transition(id, ImpactStatus.SUBMITTED, actor); }
    public ImpactResponse approve(UUID id, UUID actor) { if (!repository.isCoordinator(actor)) throw new ConflictException("A aprovação exige papel de coordenação."); return transition(id, ImpactStatus.APPROVED, actor); }
    public List<ImpactResponse> publicImpacts(UUID project) { return repository.findPublic(project); }
    private ImpactResponse transition(UUID id, ImpactStatus status, UUID actor) { try { if (repository.updateStatus(id, status, actor) != 1) throw new ConflictException("Transição de impacto inválida."); return repository.find(id); } catch (org.springframework.dao.DataAccessException ex) { throw new ConflictException("Transição de impacto inválida."); } }
    private void requireResponsible(UUID project, UUID actor) { if (!repository.isResponsible(project, actor)) throw new ConflictException("Docente fora do escopo do projeto."); }
}
