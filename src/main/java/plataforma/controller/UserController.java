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
import plataforma.dto.UserRequest;
import plataforma.dto.UserResponse;
import plataforma.service.UserService;

@RestController
@ConditionalOnProperty(prefix = "platform.crud.api", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um usuário")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista usuários (paginado)")
    public PageResponse<UserResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo id")
    public UserResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um usuário")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
