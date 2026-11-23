package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class AcademicClassService {
    private final AcademicClassRepository repository;
    public AcademicClassService(AcademicClassRepository repository) { this.repository = repository; }
    public List<AcademicClassResponse> list(UUID actor) { requireCoordinator(actor); return repository.findAll(); }
    @Transactional public AcademicClassResponse create(AcademicClassRequest request, UUID actor) { requireCoordinator(actor); try { return repository.create(request); } catch (DataIntegrityViolationException exception) { throw new ConflictException("Código de turma já cadastrado."); } }
    @Transactional public void updateStatus(UUID id, boolean active, UUID actor) { requireCoordinator(actor); repository.updateStatus(id, active); }
    private void requireCoordinator(UUID actor) { if (!repository.isCoordinator(actor)) throw new ConflictException("A operação exige papel de coordenação."); }
}
