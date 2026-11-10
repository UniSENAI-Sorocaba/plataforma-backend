package plataforma.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
 * <p>Regras implementadas: só emite com horas aprovadas que cubram a carga do edital; no máximo um
 * certificado VALID por inscrição (revogado permite nova emissão); conteúdo
 * imutável protegido por hash; revogação não apaga o registro; validação pública exige id + hash e
 * responde com o mínimo. Assinatura, validade, reemissão e PDF dependem de decisão do PO
 * (SPEC-EXT-003, AC-S3-10) e não foram inventados aqui.
 */
// TODO BE-010 (review #45, item 1): exigir usuário autenticado; emitir/revogar só docente responsável
// do edital (notice.advisor) ou coordenação; listar/buscar só os próprios (aluno) ou do edital (docente).
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
    private final String validationUrlTemplate;

    /**
     * {@code validationUrlTemplate} não tem valor padrão: sem ele a aplicação não sobe, para nenhum
     * QR Code ser impresso apontando para localhost. Deve apontar para a página de validação do
     * frontend e conter {@code {id}} e {@code {hash}}.
     */
    public CertificateService(CertificateRepository repository, CertificateMapper mapper,
            EnrollmentService enrollmentService, ActivityService activityService, CertificateHasher hasher,
            QrCodeGenerator qrCodeGenerator, Clock clock,
            @Value("${platform.certificate.validation-url-template}") String validationUrlTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.enrollmentService = enrollmentService;
        this.activityService = activityService;
        this.hasher = hasher;
        this.qrCodeGenerator = qrCodeGenerator;
        this.clock = clock;
        this.validationUrlTemplate = requireValidTemplate(validationUrlTemplate);
    }

    static String requireValidTemplate(String template) {
        if (template == null || !template.contains("{id}") || !template.contains("{hash}")) {
            throw new IllegalStateException(
                    "platform.certificate.validation-url-template deve conter {id} e {hash}");
        }
        var uri = UriComponentsBuilder.fromUriString(template)
                .buildAndExpand(Map.of("id", "x", "hash", "x")).toUri();
        boolean httpScheme = "https".equals(uri.getScheme()) || "http".equals(uri.getScheme());
        if (!httpScheme || uri.getHost() == null) {
            throw new IllegalStateException(
                    "platform.certificate.validation-url-template deve ser uma URL http(s) absoluta");
        }
        return template;
    }

    @Transactional
    public CertificateResponse issue(CertificateRequest request) {
        // A trava impede que duas emissões simultâneas da mesma inscrição gerem dois VALID.
        var enrollment = enrollmentService.findEntityForUpdate(request.enrollmentId());
        if (repository.existsByEnrollmentIdAndStatus(enrollment.getId(), CertificateStatus.VALID)) {
            throw new ConflictException("Inscrição já possui certificado válido: " + enrollment.getId());
        }
        long approvedHours = activityService.approvedHours(enrollment.getId());
        int requiredHours = enrollment.getNotice().getTotalHours();
        // Regra "horas cumpridas": aprovadas >= carga do edital. Aguarda confirmação do PO (SPEC-EXT-003).
        if (approvedHours <= 0 || approvedHours < requiredHours) {
            throw new ConflictException("Horas aprovadas abaixo da carga do edital: " + enrollment.getId());
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
        return UriComponentsBuilder.fromUriString(validationUrlTemplate)
                .buildAndExpand(Map.of("id", certificate.getId(), "hash", certificate.getValidationHash()))
                .toUriString();
    }

    private static ResourceNotFoundException notVerified() {
        return new ResourceNotFoundException("Certificado não verificado");
    }
}
