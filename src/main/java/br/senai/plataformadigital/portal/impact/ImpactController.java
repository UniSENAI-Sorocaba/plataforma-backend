package br.senai.plataformadigital.portal.impact;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/impacts")
public class ImpactController {
    private final ImpactService service;

    public ImpactController(ImpactService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ManagementResponse> create(@Valid @RequestBody DraftInput input, HttpServletRequest request) {
        Impact impact = service.create(content(input), provenance(input), correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/impacts/" + impact.id() + "/versions"))
                .eTag(etag(impact)).body(management(impact));
    }

    @PostMapping("/{id}/versions")
    ResponseEntity<ManagementResponse> revise(@PathVariable UUID id, @RequestHeader("If-Match") String revision,
                                             @Valid @RequestBody DraftInput input, HttpServletRequest request) {
        return response(service.revise(id, revision(revision), content(input), provenance(input), correlation(request)));
    }

    @PostMapping("/{id}/versions/{version}/approval")
    ResponseEntity<ManagementResponse> approve(@PathVariable UUID id, @PathVariable long version,
                                              @RequestHeader("If-Match") String revision, HttpServletRequest request) {
        return response(service.approve(id, version, revision(revision), correlation(request)));
    }

    @PutMapping("/{id}/publication")
    ResponseEntity<ManagementResponse> publish(@PathVariable UUID id, @RequestHeader("If-Match") String revision,
                                              @Valid @RequestBody PublicationInput input, HttpServletRequest request) {
        return response(service.publish(id, input.version(), revision(revision), correlation(request)));
    }

    @DeleteMapping("/{id}/publication")
    ResponseEntity<ManagementResponse> withdraw(@PathVariable UUID id, @RequestHeader("If-Match") String revision,
                                               HttpServletRequest request) {
        return response(service.withdraw(id, revision(revision), correlation(request)));
    }

    @GetMapping("/{id}/versions")
    ResponseEntity<PageResponse<VersionResponse>> versions(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        Impact impact = service.read(id);
        List<VersionResponse> versions = impact.versions().values().stream()
                .sorted(Comparator.comparingLong(Impact.Version::number)).map(version ->
                        new VersionResponse(version.number(), version.content().title(), version.content().summary(),
                                version.approval() != null)).toList();
        return ResponseEntity.ok().eTag(etag(impact)).body(page(versions, page, size));
    }

    @GetMapping("/{id}")
    PublicResponse publicImpact(@PathVariable UUID id) {
        Impact.Version version = service.publicVersion(id);
        return new PublicResponse(id, version.number(), version.content().title(), version.content().summary());
    }

    @GetMapping("/{id}/versions/{version}/provenance")
    ProvenanceResponse provenance(@PathVariable UUID id, @PathVariable long version) {
        Impact.Version found = service.provenance(id, version);
        var source = found.provenance();
        return new ProvenanceResponse(id, found.number(), source.sourceReference(), source.responsibleReference(),
                source.periodStart(), source.periodEnd(), found.createdBy(), found.createdAt(),
                found.approval() == null ? null : found.approval().actor(),
                found.approval() == null ? null : found.approval().at());
    }

    @GetMapping("/{id}/history")
    PageResponse<HistoryResponse> history(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "25") int size) {
        return page(service.history(id).stream().map(event -> new HistoryResponse(event.impactId(), event.revision(), event.action().name(),
                event.version(), event.actor(), event.at(), event.correlationId(), event.result())).toList(), page, size);
    }

    private ResponseEntity<ManagementResponse> response(Impact impact) {
        return ResponseEntity.ok().eTag(etag(impact)).body(management(impact));
    }

    private ManagementResponse management(Impact impact) {
        return new ManagementResponse(impact.id(), impact.revision(), impact.publishedVersion(),
                impact.versions().keySet().stream().mapToLong(Long::longValue).max().orElseThrow());
    }

    private <T> PageResponse<T> page(List<T> items, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ImpactFailure(400, "Paginação inválida: page >= 0 e size entre 1 e 100");
        return new PageResponse<>(items.stream().skip((long) page * size).limit(size).toList(), page, size, items.size());
    }

    private Impact.Content content(DraftInput input) { return new Impact.Content(input.title(), input.summary()); }
    private Impact.Provenance provenance(DraftInput input) {
        return new Impact.Provenance(input.provenance().sourceReference(), input.provenance().responsibleReference(),
                input.provenance().periodStart(), input.provenance().periodEnd());
    }
    private String correlation(HttpServletRequest request) { return (String) request.getAttribute("impact.correlation"); }
    private String etag(Impact impact) { return "\"" + impact.revision() + "\""; }

    private long revision(String header) {
        if (!header.matches("\"[1-9][0-9]*\"")) throw new ImpactFailure(400, "If-Match deve conter a revisão entre aspas");
        try { return Long.parseLong(header.substring(1, header.length() - 1)); }
        catch (NumberFormatException invalid) { throw new ImpactFailure(400, "Revisão inválida"); }
    }

    public interface StrictInput {
        @JsonAnySetter
        default void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("Campo não reconhecido");
        }
    }
    public record DraftInput(@NotBlank @Size(max = 200) String title,
                             @NotBlank @Size(max = 10000) String summary,
                             @NotNull @Valid ProvenanceInput provenance) implements StrictInput { }
    public record ProvenanceInput(@NotBlank @Size(max = 512) String sourceReference,
                                  @NotBlank @Size(max = 200) String responsibleReference,
                                  @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) implements StrictInput { }
    public record PublicationInput(@Positive long version) implements StrictInput { }
    public record ManagementResponse(UUID id, long revision, Long publishedVersion, long latestVersion) { }
    public record PageResponse<T>(List<T> items, int page, int size, long totalElements) { }
    public record VersionResponse(long version, String title, String summary, boolean approved) { }
    public record PublicResponse(UUID id, long version, String title, String summary) { }
    public record ProvenanceResponse(UUID id, long version, String sourceReference, String responsibleReference,
                                     LocalDate periodStart, LocalDate periodEnd, String createdBy, Instant createdAt,
                                     String approvedBy, Instant approvedAt) { }
    public record HistoryResponse(UUID impactId, long revision, String action, long version, String actor,
                                  Instant at, String correlationId, String result) { }
}
