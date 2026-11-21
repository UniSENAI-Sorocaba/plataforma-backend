package br.senai.plataformadigital.extension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CertificateServiceTest {
    @Test
    void rejectsStudentWithoutApprovedActivity() {
        var repository = Mockito.mock(CertificateRepository.class);
        var service = new CertificateService(repository);
        UUID project = UUID.randomUUID(), period = UUID.randomUUID(), student = UUID.randomUUID();
        when(repository.canIssue(project, period, student)).thenReturn(false);
        assertThatThrownBy(() -> service.issue(project, period, new CertificateRequest(student), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void issuesValidCertificateAndValidatesIt() {
        var repository = Mockito.mock(CertificateRepository.class);
        var service = new CertificateService(repository);
        UUID project = UUID.randomUUID(), period = UUID.randomUUID(), student = UUID.randomUUID();
        when(repository.canIssue(project, period, student)).thenReturn(true);
        var expected = new CertificateResponse(UUID.randomUUID(), "ABC123", project, period, student, "a".repeat(64), "ACTIVE", Instant.now(), null, "/public/certificates/ABC123");
        when(repository.insert(Mockito.eq(project), Mockito.eq(period), Mockito.eq(student), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(expected);
        when(repository.find("ABC123")).thenReturn(expected);
        service.issue(project, period, new CertificateRequest(student), UUID.randomUUID());
        service.validate("ABC123");
    }

    @Test
    void rejectsInvalidRevocationAndMissingCertificate() {
        var repository = Mockito.mock(CertificateRepository.class);
        var service = new CertificateService(repository);
        when(repository.revoke("missing", "motivo")).thenReturn(0);
        assertThatThrownBy(() -> service.revoke("missing", "motivo")).isInstanceOf(ConflictException.class);
        when(repository.find(anyString())).thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
        assertThatThrownBy(() -> service.validate("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detectsTamperedCertificateContentHash() throws Exception {
        var repository = Mockito.mock(CertificateRepository.class);
        var service = new CertificateService(repository);
        UUID project = UUID.randomUUID(), period = UUID.randomUUID(), student = UUID.randomUUID();
        String code = "ABC123";
        String canonical = project+"|"+period+"|"+student+"|"+code;
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        var intact = new CertificateResponse(UUID.randomUUID(), code, project, period, student, hash, "ACTIVE", Instant.now(), null, "/public/certificates/"+code);
        var tampered = new CertificateResponse(intact.id(), code, project, period, student, "0".repeat(64), "ACTIVE", intact.issuedAt(), null, intact.validationUrl());
        org.assertj.core.api.Assertions.assertThat(service.isContentIntact(intact)).isTrue();
        org.assertj.core.api.Assertions.assertThat(service.isContentIntact(tampered)).isFalse();
    }
}
