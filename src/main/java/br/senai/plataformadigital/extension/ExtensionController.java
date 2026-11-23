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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/v1/extension")
@ConditionalOnProperty(name = "spring.datasource.url")
public class ExtensionController {
    private final ExtensionService service;
    private final ActivityService activityService;
    private final CertificateService certificateService;
    private final AuthenticatedSubjectProvider subjects;

    public ExtensionController(ExtensionService service, ActivityService activityService, CertificateService certificateService, AuthenticatedSubjectProvider subjects) {
        this.service = service; this.activityService = activityService; this.certificateService = certificateService; this.subjects = subjects;
    }

    @PostMapping("/opportunities")
    ResponseEntity<OpportunityResponse> create(@Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor()));
    }

    @PutMapping("/opportunities/{id}")
    OpportunityResponse update(@PathVariable UUID id, @Valid @RequestBody OpportunityRequest request) {
        return service.update(id, request, actor());
    }

    @DeleteMapping("/opportunities/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, actor());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/opportunities")
    List<OpportunityResponse> publicOpportunities() { return service.publicOpportunities(); }

    @GetMapping("/opportunities/manage")
    List<OpportunityResponse> allOpportunities() { return service.allOpportunities(actor()); }

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

    @PostMapping("/projects/{projectId}/periods/{periodId}/activities")
    ResponseEntity<ActivityResponse> submitActivity(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                                     @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.submit(projectId, periodId, request, actor()));
    }

    @GetMapping("/projects/{projectId}/periods/{periodId}/activities/mine")
    List<ActivityResponse> mine(@PathVariable UUID projectId, @PathVariable UUID periodId) {
        return activityService.mine(projectId, periodId, actor());
    }

    @GetMapping("/projects/{projectId}/periods/{periodId}/activities")
    List<ActivityResponse> projectActivities(@PathVariable UUID projectId, @PathVariable UUID periodId) {
        return activityService.project(projectId, periodId, actor());
    }

    @GetMapping("/projects/{projectId}/periods/{periodId}/hours/mine")
    HoursTotalResponse total(@PathVariable UUID projectId, @PathVariable UUID periodId) {
        return activityService.total(projectId, periodId, actor());
    }

    @PutMapping("/activities/{id}")
    ActivityResponse correct(@PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
        return activityService.correct(id, request, actor());
    }

    @PatchMapping("/activities/{id}/decision")
    ActivityResponse decide(@PathVariable UUID id, @Valid @RequestBody ActivityDecisionRequest request) {
        return activityService.decide(id, request, actor());
    }

    @PostMapping("/projects/{projectId}/periods/{periodId}/certificates")
    ResponseEntity<CertificateResponse> issueCertificate(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                                          @Valid @RequestBody CertificateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.issue(projectId, periodId, request, actor()));
    }

    @PatchMapping("/certificates/{code}/revoke")
    CertificateResponse revokeCertificate(@PathVariable String code, @Valid @RequestBody CertificateRevocationRequest request) {
        return certificateService.revoke(code, request.reason());
    }

    private UUID actor() {
        return subjects.currentSubject().map(subject -> UUID.fromString(subject.id()))
                .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente."));
    }
}
