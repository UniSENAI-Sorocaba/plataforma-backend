package br.senai.plataformadigital.auth;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class FirstAccessController {
    private final org.springframework.beans.factory.ObjectProvider<FirstAccessService> firstAccess;

    public FirstAccessController(org.springframework.beans.factory.ObjectProvider<FirstAccessService> firstAccess) {
        this.firstAccess = firstAccess;
    }

    @PostMapping("/first-access")
    ResponseEntity<Void> activate(@Valid @RequestBody FirstAccessRequest request) {
        FirstAccessService service = firstAccess.getIfAvailable();
        if (service == null) return ResponseEntity.status(503).build();
        service.activate(request.cpf(), request.password());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidFirstAccessException.class)
    ResponseEntity<Map<String, Object>> invalid() {
        return ResponseEntity.badRequest().body(Map.of(
                "type", "about:blank", "title", "Bad Request", "status", 400,
                "detail", "Não foi possível ativar o primeiro acesso."));
    }

    public record FirstAccessRequest(
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @NotBlank @Size(min = 6, max = 128) String password) {
    }
}
