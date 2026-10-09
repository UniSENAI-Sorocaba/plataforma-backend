package plataforma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
import plataforma.dto.ProjectMemberRequest;
import plataforma.dto.ProjectMemberResponse;
import plataforma.service.ProjectMemberService;

@RestController
@RequestMapping("/api/v1/project-members")
@Tag(name = "Membros de projeto")
public class ProjectMemberController {

    private final ProjectMemberService service;

    public ProjectMemberController(ProjectMemberService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um membro de projeto")
    public ProjectMemberResponse create(@Valid @RequestBody ProjectMemberRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista membros de projeto (paginado)")
    public PageResponse<ProjectMemberResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um membro de projeto pelo id")
    public ProjectMemberResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um membro de projeto")
    public ProjectMemberResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectMemberRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um membro de projeto")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
