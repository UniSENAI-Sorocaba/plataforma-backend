package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class StudentAdministrationService {
    private final StudentAdministrationRepository repository;
    private final InstitutionalAuditRepository audit;

    public StudentAdministrationService(StudentAdministrationRepository repository, InstitutionalAuditRepository audit) { this.repository = repository; this.audit = audit; }

    public List<StudentResponse> list(UUID actor, String query) {
        requireCoordinator(actor);
        return repository.findAll(query);
    }

    @Transactional
    public StudentResponse create(StudentRequest request, UUID actor) {
        requireCoordinator(actor);
        try { StudentResponse result = repository.create(request, actor); audit.record(actor, "STUDENT_CREATED", "student", result.id(), "{}"); return result; }
        catch (DataIntegrityViolationException exception) { throw new ConflictException("CPF ou e-mail já cadastrado."); }
        catch (org.springframework.dao.EmptyResultDataAccessException exception) { throw new ConflictException("Turma não encontrada ou inativa."); }
    }

    @Transactional
    public void updateStatus(UUID studentId, String status, UUID actor) {
        requireCoordinator(actor);
        if (!List.of("active", "inactive").contains(status)) throw new ConflictException("Status de aluno inválido.");
        repository.updateStatus(studentId, status);
        audit.record(actor, "STUDENT_STATUS_CHANGED", "student", studentId, "{\"status\":\"" + status + "\"}");
    }

    @Transactional public StudentResponse update(UUID id, StudentUpdateRequest request, UUID actor) { requireCoordinator(actor); StudentResponse result = repository.update(id, request); audit.record(actor, "STUDENT_UPDATED", "student", id, "{}"); return result; }
    public String exportCsv(UUID actor, String query) { requireCoordinator(actor); audit.record(actor, "STUDENT_EXPORT", "student", null, "{}"); return repository.exportCsv(query); }

    public StudentImportResponse importCsv(byte[] content, UUID actor) {
        requireCoordinator(actor);
        String text = new String(content, StandardCharsets.UTF_8);
        String[] lines = text.replace("\\r", "").split("\\n");
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int total = 0;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isBlank() || index == 0 && line.toLowerCase().startsWith("nome,")) continue;
            total++;
            String[] fields = line.split(",", -1);
            if (fields.length != 5) { errors.add("Linha " + (index + 1) + ": esperado nome,email,cpf,matricula,turma."); continue; }
            try {
                create(new StudentRequest(fields[0], fields[1], fields[2].replaceAll("\\D", ""), fields[3], fields[4]), actor);
                imported++;
            } catch (RuntimeException exception) { errors.add("Linha " + (index + 1) + ": " + exception.getMessage()); }
        }
        return new StudentImportResponse(total, imported, total - imported, errors);
    }

    private void requireCoordinator(UUID actor) {
        if (!repository.isCoordinator(actor)) throw new ConflictException("A operação exige papel de coordenação.");
    }
}
