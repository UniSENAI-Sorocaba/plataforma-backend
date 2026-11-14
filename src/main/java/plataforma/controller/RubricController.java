package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import plataforma.dto.RubricCreateRequest;
import plataforma.dto.RubricVersionRequest;
import plataforma.dto.RubricVersionResponse;
import plataforma.service.RubricService;

@RestController
@ConditionalOnProperty(prefix = "platform.rubric.api", name = "enabled", havingValue = "true")
@ConditionalOnBean(RubricService.class)
@RequestMapping("/api/v1/rubrics")
@Tag(name = "Rubricas")
public class RubricController {

    private final RubricService service;

    public RubricController(RubricService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma rubrica com sua primeira versão")
    public RubricVersionResponse create(@Valid @RequestBody RubricCreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/{rubricId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma nova versão imutável da rubrica")
    public RubricVersionResponse createVersion(@PathVariable UUID rubricId,
            @Valid @RequestBody RubricVersionRequest request) {
        return service.createVersion(rubricId, request);
    }

    @GetMapping("/{rubricId}/versions/{version}")
    @Operation(summary = "Consulta uma versão de rubrica")
    public RubricVersionResponse findVersion(@PathVariable UUID rubricId, @PathVariable int version) {
        return service.findVersion(rubricId, version);
    }
}
