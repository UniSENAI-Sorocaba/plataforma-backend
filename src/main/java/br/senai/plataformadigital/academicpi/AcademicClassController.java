package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coordination/classes")
@ConditionalOnProperty(name = "spring.datasource.url")
public class AcademicClassController {
    private final AcademicClassService service;
    private final AuthenticatedSubjectProvider subjects;
    public AcademicClassController(AcademicClassService service, AuthenticatedSubjectProvider subjects) { this.service = service; this.subjects = subjects; }
    @GetMapping List<AcademicClassResponse> list() { return service.list(actor()); }
    @PostMapping AcademicClassResponse create(@Valid @RequestBody AcademicClassRequest request) { return service.create(request, actor()); }
    @PatchMapping("/{id}/status") void updateStatus(@PathVariable UUID id, @RequestParam boolean active) { service.updateStatus(id, active, actor()); }
    private UUID actor() { return subjects.currentSubject().map(subject -> UUID.fromString(subject.id())).orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")); }
}
