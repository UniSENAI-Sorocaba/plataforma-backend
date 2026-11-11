package plataforma.service;

import plataforma.dto.AuditEventFilter;
import plataforma.dto.AuditEventResponse;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.mapper.AuditEventMapper;
import plataforma.repository.AuditEventRepository;
import plataforma.repository.AuditEventSpecifications;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** API pública de auditoria para os demais módulos. */
@Service
@Validated
@Transactional(readOnly = true)
public class AuditEventService {

    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;
    private final Clock clock;

    public AuditEventService(AuditEventRepository repository, AuditEventMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public AuditEventResponse record(@Valid RecordAuditEventRequest request) {
        var event = mapper.toEntity(request, UUID.randomUUID(), Instant.now(clock));
        return mapper.toResponse(repository.save(event));
    }

    public AuditEventResponse findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Evento de auditoria não encontrado: " + id));
    }

    public PageResponse<AuditEventResponse> search(AuditEventFilter filter, Pageable pageable) {
        var page = repository.findAll(AuditEventSpecifications.matching(filter), pageable);
        return PageResponse.from(page, mapper::toResponse);
    }
}
