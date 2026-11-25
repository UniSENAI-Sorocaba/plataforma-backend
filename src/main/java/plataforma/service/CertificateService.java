package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.CertificateMapper;
import plataforma.model.Certificate;
import plataforma.repository.CertificateRepository;

// TODO BE-017: emitir somente com horas cumpridas, gerar hash/QR e validação pública.
@Service
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository repository;
    private final CertificateMapper mapper;
    private final EnrollmentService enrollmentService;

    public CertificateService(CertificateRepository repository, CertificateMapper mapper,
            EnrollmentService enrollmentService) {
        this.repository = repository;
        this.mapper = mapper;
        this.enrollmentService = enrollmentService;
    }

    @Transactional
    public CertificateResponse create(CertificateRequest request) {
        var certificate = new Certificate();
        copyToEntity(request, certificate);
        return mapper.toResponse(repository.saveAndFlush(certificate));
    }

    public PageResponse<CertificateResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public CertificateResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public CertificateResponse update(UUID id, CertificateRequest request) {
        var certificate = findEntity(id);
        copyToEntity(request, certificate);
        return mapper.toResponse(repository.saveAndFlush(certificate));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public Certificate findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado: " + id));
    }

    private void copyToEntity(CertificateRequest request, Certificate certificate) {
        mapper.copyToEntity(request, certificate);
        certificate.setEnrollment(enrollmentService.findEntity(request.enrollmentId()));
    }
}
