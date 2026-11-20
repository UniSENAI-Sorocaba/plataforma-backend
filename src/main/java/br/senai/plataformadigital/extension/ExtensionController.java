package br.senai.plataformadigital.extension;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/extension")
@ConditionalOnProperty(name = "spring.datasource.url")
public class ExtensionController {
    private final ExtensionService service;
    private final AuthenticatedSubjectProvider subjects;

    public ExtensionController(ExtensionService service, AuthenticatedSubjectProvider subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @PostMapping("/opportunities")
    ResponseEntity<OpportunityResponse> create(@Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor()));
    }

    @GetMapping("/opportunities")
    List<OpportunityResponse> publicOpportunities() { return service.publicOpportunities(); }

    @GetMapping("/opportunities/{id}")
    OpportunityResponse get(@PathVariable UUID id) { return service.publicOpportunity(id); }

    @PatchMapping("/opportunities/{id}/publish")
    OpportunityResponse publish(@PathVariable UUID id) { return service.publish(id, actor()); }

    @PatchMapping("/opportunities/{id}/close")
    OpportunityResponse close(@PathVariable UUID id) { return service.close(id, actor()); }

    @PostMapping("/opportunities/{id}/applications")
    ResponseEntity<ApplicationResponse> apply(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.apply(id, actor()));
    }

    @GetMapping("/opportunities/{id}/applications")
    List<ApplicationResponse> opportunityApplications(@PathVariable UUID id) {
        return service.opportunityApplications(id);
    }

    @GetMapping("/applications/mine")
    List<ApplicationResponse> ownApplications() { return service.ownApplications(actor()); }

    @PostMapping("/applications/{id}/withdraw")
    ApplicationResponse withdraw(@PathVariable UUID id) { return service.withdraw(id, actor()); }

    private UUID actor() {
        return subjects.currentSubject().map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente."));
    }
}
