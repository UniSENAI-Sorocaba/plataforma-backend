package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/periods/{periodId}/evaluations")
@ConditionalOnProperty(name = "spring.datasource.url")
public class EvaluationController {
    private final EvaluationService service;
    private final AuthenticatedSubjectProvider subjects;

    public EvaluationController(EvaluationService service, AuthenticatedSubjectProvider subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @PostMapping
    ResponseEntity<EvaluationResponse> create(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                               @Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectId, periodId, request, actor()));
    }

    @GetMapping
    List<EvaluationResponse> list(@PathVariable UUID projectId, @PathVariable UUID periodId) {
        return service.list(projectId, periodId, actor());
    }

    @PostMapping("/{evaluationId}/grade")
    EvaluationGradeResponse grade(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                   @PathVariable UUID evaluationId, @Valid @RequestBody EvaluationGradeRequest request) {
        return service.grade(projectId, periodId, evaluationId, request, actor());
    }

    private UUID actor() {
        return subjects.currentSubject().map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente."));
    }
}
