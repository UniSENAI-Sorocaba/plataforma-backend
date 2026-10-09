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
import plataforma.dto.ExaminationBoardRequest;
import plataforma.dto.ExaminationBoardResponse;
import plataforma.service.ExaminationBoardService;

@RestController
@RequestMapping("/api/v1/boards")
@Tag(name = "Bancas")
public class ExaminationBoardController {

    private final ExaminationBoardService service;

    public ExaminationBoardController(ExaminationBoardService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma banca")
    public ExaminationBoardResponse create(@Valid @RequestBody ExaminationBoardRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista bancas (paginado)")
    public PageResponse<ExaminationBoardResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma banca pelo id")
    public ExaminationBoardResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma banca")
    public ExaminationBoardResponse update(@PathVariable UUID id, @Valid @RequestBody ExaminationBoardRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma banca")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
