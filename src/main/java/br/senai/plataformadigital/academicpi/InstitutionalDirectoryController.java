package br.senai.plataformadigital.academicpi;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institutional/directory")
@ConditionalOnProperty(name = "spring.datasource.url")
public class InstitutionalDirectoryController {
    private final InstitutionalDirectoryRepository repository;

    public InstitutionalDirectoryController(InstitutionalDirectoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<InstitutionalDirectoryEntry> list(@RequestParam(required = false) String role,
                                           @RequestParam(required = false) String q) {
        return repository.find(role, q);
    }
}
