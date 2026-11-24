package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations")
@ConditionalOnProperty(name = "spring.datasource.url")
public class AssignedEvaluationController {
    private final EvaluationService service;
    private final AuthenticatedSubjectProvider subjects;

    public AssignedEvaluationController(EvaluationService service, AuthenticatedSubjectProvider subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @GetMapping("/assigned")
    List<EvaluationResponse> assigned() {
        return service.listAssigned(subjects.currentSubject()
                .map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")));
    }

    @GetMapping("/student")
    List<EvaluationResponse> student() {
        return service.listForStudent(subjects.currentSubject()
                .map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")));
    }

    @GetMapping("/coordination")
    List<CoordinationEvaluationResponse> coordination() {
        return service.listForCoordination(subjects.currentSubject()
                .map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")));
    }
}
