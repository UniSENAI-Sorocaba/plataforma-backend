package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "spring.datasource.url")
public class ImpactController {
    private final ImpactService service;
    private final AuthenticatedSubjectProvider subjects;
    public ImpactController(ImpactService service, AuthenticatedSubjectProvider subjects) { this.service = service; this.subjects = subjects; }
    @PostMapping("/projects/{projectId}/impacts")
    ResponseEntity<ImpactResponse> create(@PathVariable UUID projectId, @Valid @RequestBody ImpactRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectId, request, actor())); }
    @PatchMapping("/impacts/{id}/submit")
    ImpactResponse submit(@PathVariable UUID id) { return service.submit(id, actor()); }
    @PatchMapping("/impacts/{id}/approve")
    ImpactResponse approve(@PathVariable UUID id) { return service.approve(id, actor()); }
    @GetMapping("/public/impacts")
    List<ImpactResponse> publicImpacts(@RequestParam(required = false) UUID projectId) { return service.publicImpacts(projectId); }
    private UUID actor() { return subjects.currentSubject().map(subject -> UUID.fromString(subject.id())).orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")); }
}
