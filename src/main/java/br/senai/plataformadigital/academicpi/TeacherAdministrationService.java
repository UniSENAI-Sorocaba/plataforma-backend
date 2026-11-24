package br.senai.plataformadigital.academicpi;
import br.senai.plataformadigital.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
@Service @ConditionalOnProperty(name="spring.datasource.url")
public class TeacherAdministrationService {
    private final StudentAdministrationRepository repository; private final InstitutionalAuditRepository audit;
    public TeacherAdministrationService(StudentAdministrationRepository repository, InstitutionalAuditRepository audit) { this.repository=repository; this.audit=audit; }
    public List<TeacherResponse> list(UUID actor, String q) { require(actor); return repository.findTeachers(q); }
    public TeacherResponse create(StudentRequest request, UUID actor) { require(actor); try { TeacherResponse result=repository.createTeacher(request, actor); audit.record(actor,"TEACHER_CREATED","teacher",result.id(),"{}"); return result; } catch(DataIntegrityViolationException e){ throw new ConflictException("CPF ou e-mail já cadastrado."); } }
    private void require(UUID actor){ if(!repository.isCoordinator(actor)) throw new ConflictException("A operação exige papel de coordenação."); }
}
