package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "spring.datasource.url")
public class ProjectController {
    private final ProjectService service;
    private final AuthenticatedSubjectProvider subjects;

    public ProjectController(ProjectService service, AuthenticatedSubjectProvider subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @PostMapping("/projects")
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor()));
    }

    @GetMapping("/projects/{id}")
    ProjectResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/public/projects")
    List<ProjectResponse> publicProjects() {
        return service.publicProjects();
    }

    @GetMapping("/public/projects/search")
    PublicProjectPage publicProjectSearch(@RequestParam(required = false) String course,
            @RequestParam(required = false) String semester, @RequestParam(required = false) String ods,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return service.publicProjectSearch(course, semester, ods, page, pageSize);
    }

    @GetMapping("/projects/managed")
    List<ProjectResponse> managedProjects() {
        return service.managedProjects(actor());
    }

    @GetMapping("/coordination/projects")
    List<ProjectResponse> coordinationProjects() { return service.coordinationProjects(actor()); }

    @PatchMapping("/coordination/projects/{id}")
    ProjectResponse updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
        return service.updateProject(id, request, actor());
    }

    @DeleteMapping("/coordination/projects/{id}")
    ResponseEntity<Void> archiveProject(@PathVariable UUID id) {
        service.archiveProject(id, actor());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/projects/{id}/enrollment-periods")
    ResponseEntity<EnrollmentPeriodResponse> createPeriod(@PathVariable UUID id,
                                                           @Valid @RequestBody EnrollmentPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPeriod(id, request, actor()));
    }

    @GetMapping("/projects/{id}/enrollment-periods")
    List<EnrollmentPeriodResponse> periods(@PathVariable UUID id) {
        return service.periods(id, actor());
    }

    @PatchMapping("/projects/{id}/enrollment-periods/{periodId}")
    EnrollmentPeriodResponse updatePeriod(@PathVariable UUID id, @PathVariable UUID periodId,
                                           @Valid @RequestBody EnrollmentPeriodUpdateRequest request) {
        return service.updatePeriod(id, periodId, request, actor());
    }

    @GetMapping("/projects/{id}/participation-requests")
    List<ParticipationRequestResponse> projectRequests(@PathVariable UUID id) {
        return service.projectRequests(id, actor());
    }

    @PatchMapping("/projects/{id}/publication")
    ProjectResponse publication(@PathVariable UUID id, @Valid @RequestBody PublicationRequest request) {
        return service.changePublication(id, request.publicationStatus(), actor());
    }

    @PatchMapping("/projects/{id}/lifecycle")
    ProjectResponse lifecycle(@PathVariable UUID id, @Valid @RequestBody LifecycleRequest request) {
        return service.changeLifecycle(id, request.lifecycleStatus(), actor());
    }

    @PostMapping("/projects/{id}/participation-requests")
    ResponseEntity<ParticipationRequestResponse> requestParticipation(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.requestParticipation(id, actor()));
    }

    @PatchMapping("/participation-requests/{id}")
    ParticipationRequestResponse decide(@PathVariable UUID id, @Valid @RequestBody ParticipationDecisionRequest request) {
        return service.decide(id, request.status(), actor());
    }

    @PostMapping("/participation-requests/{id}/withdraw")
    ParticipationRequestResponse withdraw(@PathVariable UUID id) {
        return service.withdraw(id, actor());
    }

    @GetMapping("/participation-requests/mine")
    List<ParticipationRequestResponse> ownRequests() {
        return service.ownRequests(actor());
    }

    @GetMapping("/projects/{projectId}/periods/{periodId}/participants")
    List<ParticipantResponse> participants(@PathVariable UUID projectId, @PathVariable UUID periodId) {
        return service.participants(projectId, periodId, actor());
    }

    @DeleteMapping("/projects/{projectId}/periods/{periodId}/participants/{studentId}")
    ResponseEntity<Void> remove(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                @PathVariable UUID studentId, @RequestBody RemovalRequest request) {
        service.remove(projectId, periodId, studentId, request == null ? null : request.reason(), actor());
        return ResponseEntity.noContent().build();
    }

    private UUID actor() {
        return subjects.currentSubject().map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente."));
    }

    public record PublicationRequest(@NotNull ProjectPublicationStatus publicationStatus) { }
    public record LifecycleRequest(@NotNull ProjectLifecycleStatus lifecycleStatus) { }
    public record RemovalRequest(String reason) { }
}
