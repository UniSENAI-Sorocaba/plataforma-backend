package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "spring.datasource.url")
public class DeliverableController {
    private final DeliverableService service;
    private final AuthenticatedSubjectProvider subjects;

    public DeliverableController(DeliverableService service, AuthenticatedSubjectProvider subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @PostMapping("/projects/{projectId}/periods/{periodId}/deliverables")
    ResponseEntity<DeliverableResponse> initiate(@PathVariable UUID projectId, @PathVariable UUID periodId,
                                                  @Valid @RequestBody DeliverableInitiationRequest request) {
        return ResponseEntity.status(201).body(service.initiate(projectId, periodId, request, actor()));
    }

    @PutMapping(value = "/deliverables/{id}/content", consumes = MediaType.ALL_VALUE)
    ResponseEntity<DeliverableResponse> upload(@PathVariable UUID id, @RequestParam String token,
                                               @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                               @RequestBody byte[] content) {
        return ResponseEntity.ok(service.upload(id, token, contentType, content));
    }

    @org.springframework.web.bind.annotation.GetMapping("/deliverables/{id}/download")
    ResponseEntity<ByteArrayResource> download(@PathVariable UUID id, @RequestParam String token) {
        DeliverableService.DownloadedDeliverable file = service.download(id, token);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(new ByteArrayResource(file.content()));
    }

    private UUID actor() { return subjects.currentSubject().map(subject -> UUID.fromString(subject.id())).orElseThrow(); }
}
