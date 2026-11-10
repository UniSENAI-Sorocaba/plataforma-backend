package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import plataforma.dto.CertificateRequest;
import plataforma.dto.CertificateRevocationRequest;
import plataforma.exception.ConflictException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.CertificateMapper;
import plataforma.model.Certificate;
import plataforma.model.CertificateStatus;
import plataforma.model.Enrollment;
import plataforma.model.Notice;
import plataforma.model.UserAccount;
import plataforma.repository.CertificateRepository;

class CertificateServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00.987654Z");
    private static final String URL_TEMPLATE = "https://plataforma.test/certificados/validar?id={id}&hash={hash}";

    private final UUID enrollmentId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID noticeId = UUID.randomUUID();

    private CertificateRepository repository;
    private EnrollmentService enrollmentService;
    private ActivityService activityService;
    private CertificateHasher hasher;
    private CertificateService service;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        repository = mock(CertificateRepository.class);
        enrollmentService = mock(EnrollmentService.class);
        activityService = mock(ActivityService.class);
        hasher = new CertificateHasher();
        service = new CertificateService(repository, new CertificateMapper(), enrollmentService, activityService,
                hasher, new QrCodeGenerator(), Clock.fixed(NOW, ZoneOffset.UTC), URL_TEMPLATE);

        enrollment = enrollment(enrollmentId, studentId, noticeId);
        when(enrollmentService.findEntityForUpdate(enrollmentId)).thenReturn(enrollment);
        when(repository.saveAndFlush(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate certificate = invocation.getArgument(0);
            if (certificate.getId() == null) {
                ReflectionTestUtils.setField(certificate, "id", UUID.randomUUID());
            }
            return certificate;
        });
    }

    @Test
    void emiteComHorasAprovadasEHashDoConteudo() {
        when(activityService.approvedHours(enrollmentId)).thenReturn(40L);

        var response = service.issue(new CertificateRequest(enrollmentId));

        var issuedAt = Instant.parse("2026-11-09T15:00:00Z");
        assertThat(response.hours()).isEqualTo(40);
        assertThat(response.status()).isEqualTo(CertificateStatus.VALID);
        assertThat(response.issuedAt()).isEqualTo(issuedAt);
        assertThat(response.validationHash()).isEqualTo(hasher.hash(enrollmentId, studentId, noticeId, 40, issuedAt));
        assertThat(response.validationUrl()).isEqualTo("https://plataforma.test/certificados/validar?id="
                + response.id() + "&hash=" + response.validationHash());
        assertThat(response.revokedAt()).isNull();
    }

    @Test
    void naoEmiteSemHorasAprovadas() {
        when(activityService.approvedHours(enrollmentId)).thenReturn(0L);

        assertThatThrownBy(() -> service.issue(new CertificateRequest(enrollmentId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("abaixo da carga do edital");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void naoEmiteSegundoCertificadoValidoParaAMesmaInscricao() {
        when(repository.existsByEnrollmentIdAndStatus(enrollmentId, CertificateStatus.VALID)).thenReturn(true);

        assertThatThrownBy(() -> service.issue(new CertificateRequest(enrollmentId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui certificado válido");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void naoEmiteComHorasAbaixoDaCargaDoEdital() {
        when(activityService.approvedHours(enrollmentId)).thenReturn(39L);

        assertThatThrownBy(() -> service.issue(new CertificateRequest(enrollmentId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("abaixo da carga do edital");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void emiteComHorasIguaisOuAcimaDaCargaUsandoAsHorasAprovadas() {
        when(activityService.approvedHours(enrollmentId)).thenReturn(40L, 55L);

        assertThat(service.issue(new CertificateRequest(enrollmentId)).hours()).isEqualTo(40);
        assertThat(service.issue(new CertificateRequest(enrollmentId)).hours()).isEqualTo(55);
    }

    @Test
    void revogadoNaoImpedeNovaEmissao() {
        // Só certificado VALID bloqueia: com o anterior revogado, a consulta por VALID retorna false.
        when(repository.existsByEnrollmentIdAndStatus(enrollmentId, CertificateStatus.VALID)).thenReturn(false);
        when(activityService.approvedHours(enrollmentId)).thenReturn(40L);

        assertThat(service.issue(new CertificateRequest(enrollmentId)).status()).isEqualTo(CertificateStatus.VALID);
        verify(repository).existsByEnrollmentIdAndStatus(enrollmentId, CertificateStatus.VALID);
    }

    @Test
    void templateDeUrlDeValidacaoEhObrigatorioEAbsoluto() {
        assertThatThrownBy(() -> CertificateService.requireValidTemplate(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CertificateService.requireValidTemplate("https://plataforma.test/validar?id={id}"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CertificateService.requireValidTemplate("/validar/{id}?hash={hash}"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CertificateService.requireValidTemplate("ftp://plataforma.test/{id}/{hash}"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(CertificateService.requireValidTemplate(URL_TEMPLATE)).isEqualTo(URL_TEMPLATE);
    }

    @Test
    void inscricaoInexistenteRetornaNotFound() {
        var unknown = UUID.randomUUID();
        when(enrollmentService.findEntityForUpdate(unknown)).thenThrow(new ResourceNotFoundException("Inscrição não encontrada"));

        assertThatThrownBy(() -> service.issue(new CertificateRequest(unknown)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revogaMantendoORegistroERejeitaSegundaRevogacao() {
        var certificate = stored(40);

        var response = service.revoke(certificate.getId(), new CertificateRevocationRequest("  Horas lançadas por engano "));

        assertThat(response.status()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(response.revokedAt()).isEqualTo(NOW);
        assertThat(response.revocationReason()).isEqualTo("Horas lançadas por engano");
        assertThatThrownBy(() -> service.revoke(certificate.getId(), new CertificateRevocationRequest("de novo")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validacaoPublicaRetornaSituacaoAtual() {
        var certificate = stored(40);

        var valid = service.validate(certificate.getId(), certificate.getValidationHash());
        assertThat(valid.status()).isEqualTo(CertificateStatus.VALID);
        assertThat(valid.revokedAt()).isNull();

        certificate.revoke(NOW, "motivo interno");
        var revoked = service.validate(certificate.getId(), certificate.getValidationHash().toUpperCase());
        assertThat(revoked.status()).isEqualTo(CertificateStatus.REVOKED);
        assertThat(revoked.revokedAt()).isEqualTo(NOW);
    }

    @Test
    void validacaoFalhaComHashDivergenteOuIdInexistente() {
        var certificate = stored(40);
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(certificate.getId(), "0".repeat(64)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Certificado não verificado");
        assertThatThrownBy(() -> service.validate(unknown, certificate.getValidationHash()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Certificado não verificado");
    }

    @Test
    void validacaoFalhaQuandoORegistroFoiAdulteradoNoBanco() {
        var certificate = stored(40);
        ReflectionTestUtils.setField(certificate, "hours", 400);

        assertThatThrownBy(() -> service.validate(certificate.getId(), certificate.getValidationHash()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Certificado não verificado");
    }

    @Test
    void qrCodeApontaParaAValidacaoPublica() {
        var certificate = stored(40);

        assertThat(service.qrCode(certificate.getId())).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    private Certificate stored(int hours) {
        var issuedAt = Instant.parse("2026-11-01T12:00:00Z");
        var certificate = Certificate.issue(enrollment, hours, issuedAt,
                hasher.hash(enrollmentId, studentId, noticeId, hours, issuedAt));
        var id = UUID.randomUUID();
        ReflectionTestUtils.setField(certificate, "id", id);
        when(repository.findById(id)).thenReturn(Optional.of(certificate));
        return certificate;
    }

    private static Enrollment enrollment(UUID enrollmentId, UUID studentId, UUID noticeId) {
        var student = new UserAccount();
        ReflectionTestUtils.setField(student, "id", studentId);
        var notice = new Notice();
        ReflectionTestUtils.setField(notice, "id", noticeId);
        notice.setTotalHours(40);
        var enrollment = new Enrollment();
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
        enrollment.setStudent(student);
        enrollment.setNotice(notice);
        return enrollment;
    }
}
