package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rubrics")
@ConditionalOnProperty(name = "spring.datasource.url")
public class RubricController {
    private final RubricService service;
    private final AuthenticatedSubjectProvider subjects;
    public RubricController(RubricService service, AuthenticatedSubjectProvider subjects) { this.service = service; this.subjects = subjects; }
    @PostMapping
    ResponseEntity<RubricVersionResponse> create(@Valid @RequestBody RubricCreateRequest request) { return ResponseEntity.status(201).body(service.create(request, actor())); }
    @GetMapping("/{rubricId}/versions/{version}")
    RubricVersionResponse get(@PathVariable UUID rubricId, @PathVariable int version) { return service.get(rubricId, version); }
    private UUID actor() { return subjects.currentSubject().map(s -> UUID.fromString(s.id())).orElseThrow(); }
}
