package plataforma.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateResponse;
import plataforma.dto.CertificateRevocationRequest;
import plataforma.dto.CertificateValidationResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ConflictException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.CertificateMapper;
import plataforma.model.Certificate;
import plataforma.model.CertificateStatus;
import plataforma.repository.CertificateRepository;

/**
 * Emissão, revogação e validação de certificados (BE-017).
 *
 * <p>Regras implementadas: só emite com horas aprovadas; um certificado por inscrição; conteúdo
 * imutável protegido por hash; revogação não apaga o registro; validação pública exige id + hash e
 * responde com o mínimo. Assinatura, validade, reemissão e PDF dependem de decisão do PO
 * (SPEC-EXT-003, AC-S3-10) e não foram inventados aqui.
 */
// TODO BE-010: restringir emissão e revogação por papel quando o RBAC estiver integrado.
@Service
@Transactional(readOnly = true)
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository repository;
    private final CertificateMapper mapper;
    private final EnrollmentService enrollmentService;
    private final ActivityService activityService;
    private final CertificateHasher hasher;
    private final QrCodeGenerator qrCodeGenerator;
    private final Clock clock;
    private final String validationBaseUrl;

    public CertificateService(CertificateRepository repository, CertificateMapper mapper,
            EnrollmentService enrollmentService, ActivityService activityService, CertificateHasher hasher,
            QrCodeGenerator qrCodeGenerator, Clock clock,
            @Value("${platform.certificate.validation-base-url:http://localhost:8080}") String validationBaseUrl) {
        this.repository = repository;
        this.mapper = mapper;
        this.enrollmentService = enrollmentService;
        this.activityService = activityService;
        this.hasher = hasher;
        this.qrCodeGenerator = qrCodeGenerator;
        this.clock = clock;
        this.validationBaseUrl = validationBaseUrl;
    }

    @Transactional
    public CertificateResponse issue(CertificateRequest request) {
        var enrollment = enrollmentService.findEntity(request.enrollmentId());
        if (repository.existsByEnrollmentId(enrollment.getId())) {
            throw new ConflictException("Inscrição já possui certificado: " + enrollment.getId());
        }
        long approvedHours = activityService.approvedHours(enrollment.getId());
        if (approvedHours <= 0) {
            throw new ConflictException("Inscrição sem horas aprovadas: " + enrollment.getId());
        }
        int hours = Math.toIntExact(approvedHours);
        // Segundos inteiros: o valor volta igual do banco e o hash pode ser recalculado depois.
        Instant issuedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        String hash = hasher.hash(enrollment.getId(), enrollment.getStudent().getId(),
                enrollment.getNotice().getId(), hours, issuedAt);

        var certificate = repository.saveAndFlush(Certificate.issue(enrollment, hours, issuedAt, hash));
        log.info("Certificado emitido: certificateId={} hours={}", certificate.getId(), hours);
        return toResponse(certificate);
    }

    public PageResponse<CertificateResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), this::toResponse);
    }

    public CertificateResponse findById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public CertificateResponse revoke(UUID id, CertificateRevocationRequest request) {
        var certificate = findEntity(id);
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new ConflictException("Certificado já revogado: " + id);
        }
        certificate.revoke(Instant.now(clock), request.reason().strip());
        log.info("Certificado revogado: certificateId={}", id);
        return toResponse(repository.saveAndFlush(certificate));
    }

    /** PNG do QR Code que aponta para a validação pública do certificado. */
    public byte[] qrCode(UUID id) {
        return qrCodeGenerator.png(validationUrl(findEntity(id)));
    }

    /**
     * Validação pública. Id inexistente, hash diferente do emitido e registro adulterado no banco
     * respondem da mesma forma (404), para a consulta não servir de oráculo.
     */
    public CertificateValidationResponse validate(UUID id, String hash) {
        var certificate = repository.findById(id).orElseThrow(CertificateService::notVerified);
        if (!hasher.matches(certificate.getValidationHash(), hash)) {
            throw notVerified();
        }
        if (!hasher.matches(certificate.getValidationHash(), recomputeHash(certificate))) {
            log.warn("Certificado com conteúdo divergente do hash emitido: certificateId={}", id);
            throw notVerified();
        }
        return mapper.toValidationResponse(certificate);
    }

    public Certificate findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado: " + id));
    }

    private String recomputeHash(Certificate certificate) {
        var enrollment = certificate.getEnrollment();
        return hasher.hash(enrollment.getId(), enrollment.getStudent().getId(), enrollment.getNotice().getId(),
                certificate.getHours(), certificate.getIssuedAt());
    }

    private CertificateResponse toResponse(Certificate certificate) {
        return mapper.toResponse(certificate, validationUrl(certificate));
    }

    private String validationUrl(Certificate certificate) {
        return UriComponentsBuilder.fromUriString(validationBaseUrl)
                .path("/api/v1/public/certificates/{id}")
                .queryParam("hash", certificate.getValidationHash())
                .buildAndExpand(certificate.getId())
                .toUriString();
    }

    private static ResourceNotFoundException notVerified() {
        return new ResourceNotFoundException("Certificado não verificado");
    }
}
