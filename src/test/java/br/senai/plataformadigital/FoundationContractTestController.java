package br.senai.plataformadigital;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/foundation/errors")
@Validated
class FoundationContractTestController {
    @GetMapping("/not-found")
    ResponseEntity<Void> notFound() {
        throw new ResourceNotFoundException("Recurso de teste não encontrado.");
    }

    @GetMapping("/conflict")
    ResponseEntity<Void> conflict() {
        throw new ConflictException("Conflito de teste.");
    }

    @PostMapping("/validation")
    ResponseEntity<Void> validation(@Valid @RequestBody Payload payload) {
        return ResponseEntity.ok().build();
    }

    record Payload(@NotBlank String name) {
    }
}
