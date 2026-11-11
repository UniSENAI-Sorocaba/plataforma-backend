package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import plataforma.dto.PageResponse;
import plataforma.dto.ActivityRequest;
import plataforma.dto.ActivityResponse;
import plataforma.service.ActivityService;

@RestController
@ConditionalOnProperty(prefix = "platform.crud.api", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/activities")
@Tag(name = "Atividades de extensão")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma atividade")
    public ActivityResponse create(@Valid @RequestBody ActivityRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista atividades (paginado)")
    public PageResponse<ActivityResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma atividade pelo id")
    public ActivityResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma atividade")
    public ActivityResponse update(@PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma atividade")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
