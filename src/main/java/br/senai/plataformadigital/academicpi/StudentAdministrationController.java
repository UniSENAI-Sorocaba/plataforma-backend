package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/coordination/students")
@ConditionalOnProperty(name = "spring.datasource.url")
public class StudentAdministrationController {
    private final StudentAdministrationService service;
    private final AuthenticatedSubjectProvider subjects;

    public StudentAdministrationController(StudentAdministrationService service, AuthenticatedSubjectProvider subjects) {
        this.service = service; this.subjects = subjects;
    }

    @GetMapping
    List<StudentResponse> list(@RequestParam(required = false) String q) { return service.list(actor(), q); }

    @PostMapping
    StudentResponse create(@Valid @RequestBody StudentRequest request) { return service.create(request, actor()); }

    @PatchMapping("/{studentId}/status")
    void updateStatus(@PathVariable UUID studentId, @RequestParam String value) { service.updateStatus(studentId, value, actor()); }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    StudentImportResponse importCsv(@RequestPart("file") MultipartFile file) throws java.io.IOException {
        return service.importCsv(file.getBytes(), actor());
    }

    @PutMapping("/{studentId}")
    StudentResponse update(@PathVariable UUID studentId, @Valid @RequestBody StudentUpdateRequest request) { return service.update(studentId, request, actor()); }

    @GetMapping(value = "/export", produces = "text/csv")
    ResponseEntity<byte[]> export(@RequestParam(required = false) String q) { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alunos.csv").body(service.exportCsv(actor(), q).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    private UUID actor() { return subjects.currentSubject().map(subject -> UUID.fromString(subject.id()))
            .orElseThrow(() -> new IllegalStateException("Identidade autenticada ausente.")); }
}
