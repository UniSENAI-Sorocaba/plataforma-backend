package br.senai.plataformadigital.academicpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliverableServiceTest {
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID PERIOD = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Mock DeliverableRepository repository;
    @Mock DeliverableStorage storage;
    private DeliverableService service;

    @BeforeEach
    void setUp() { service = new DeliverableService(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC)); }

    @Test
    void rejectsUnsupportedTypeBeforeTouchingStorage() {
        DeliverableInitiationRequest request = new DeliverableInitiationRequest("malware.exe", "application/octet-stream", 10L);

        assertThatThrownBy(() -> service.initiate(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("MIME");
        verify(repository, never()).insert(any(), any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void rejectsFilesAboveTheApprovedTwentyMiBLimit() {
        DeliverableInitiationRequest request = new DeliverableInitiationRequest("trabalho.pdf", "application/pdf", 20L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.initiate(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("20 MB");
        verify(repository, never()).insert(any(), any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void rejectsTypesOutsideTheApprovedMvpFormats() {
        DeliverableInitiationRequest request = new DeliverableInitiationRequest("dados.csv", "text/csv", 10L);

        assertThatThrownBy(() -> service.initiate(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("MIME");
    }

    @Test
    void rejectsNonParticipant() {
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(false);
        DeliverableInitiationRequest request = new DeliverableInitiationRequest("trabalho.pdf", "application/pdf", 10L);

        assertThatThrownBy(() -> service.initiate(PROJECT, PERIOD, request, STUDENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("participante");
    }

    @Test
    void uploadsAndCreatesDownloadToken() throws Exception {
        when(repository.isActiveParticipant(PROJECT, PERIOD, STUDENT)).thenReturn(true);
        when(repository.insert(any(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        UUID id = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        DeliverableResponse pending = new DeliverableResponse(id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 4,
                null, 1, DeliverableStatus.PENDING_UPLOAD, NOW, null, NOW.plusSeconds(900), null, null);
        when(repository.find(id)).thenReturn(pending);
        DeliverableRepository.DeliverableRecord record = new DeliverableRepository.DeliverableRecord(
                id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 4, null, 1,
                DeliverableStatus.PENDING_UPLOAD, "deliverables/test", NOW.plusSeconds(900), null);
        when(repository.findForToken(any(), anyString(), anyString())).thenReturn(record);
        when(repository.find(id)).thenReturn(new DeliverableResponse(id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 4,
                "4a7d1ed414474e4033ac29ccb8653d9b", 1, DeliverableStatus.AVAILABLE, NOW, NOW, NOW.plusSeconds(900), null, null));

        DeliverableResponse initiated = service.initiate(PROJECT, PERIOD,
                new DeliverableInitiationRequest("trabalho.pdf", "application/pdf", 4L), STUDENT);
        assertThat(initiated.uploadUrl()).contains("/content?token=");
        DeliverableResponse uploaded = service.upload(id, "upload-token", "application/pdf", new byte[] {1, 2, 3, 4});

        assertThat(uploaded.status()).isEqualTo(DeliverableStatus.AVAILABLE);
        assertThat(uploaded.downloadUrl()).contains("/download?token=");
        verify(storage).write("deliverables/test", new byte[] {1, 2, 3, 4});
        verify(repository).markAvailable(any(), anyString(), anyString(), any());
    }

    @Test
    void rejectsWrongPayloadSize() throws Exception {
        DeliverableRepository.DeliverableRecord record = new DeliverableRepository.DeliverableRecord(
                UUID.randomUUID(), PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 4, null, 1,
                DeliverableStatus.PENDING_UPLOAD, "deliverables/test", NOW.plusSeconds(900), null);
        when(repository.findForToken(any(), anyString(), anyString())).thenReturn(record);

        assertThatThrownBy(() -> service.upload(record.id(), "token", "application/pdf", new byte[] {1}))
                .isInstanceOf(ConflictException.class).hasMessageContaining("tamanho");
        verify(storage, never()).write(anyString(), any());
    }

    @Test
    void rejectsWrongContentTypeAndExpiredUpload() {
        UUID id = UUID.randomUUID();
        DeliverableRepository.DeliverableRecord record = new DeliverableRepository.DeliverableRecord(
                id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 1, null, 1,
                DeliverableStatus.PENDING_UPLOAD, "deliverables/test", NOW.minusSeconds(1), null);
        when(repository.findForToken(any(), anyString(), anyString())).thenReturn(record);

        assertThatThrownBy(() -> service.upload(id, "token", "text/plain", new byte[] {1}))
                .isInstanceOf(ConflictException.class).hasMessageContaining("expirou");
    }

    @Test
    void rejectsExpiredDownload() {
        UUID id = UUID.randomUUID();
        DeliverableRepository.DeliverableRecord record = new DeliverableRepository.DeliverableRecord(
                id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 1, "a".repeat(64), 1,
                DeliverableStatus.AVAILABLE, "deliverables/test", NOW.minusSeconds(900), NOW.minusSeconds(1));
        when(repository.findForToken(any(), anyString(), anyString())).thenReturn(record);

        assertThatThrownBy(() -> service.download(id, "token"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("expirou");
    }

    @Test
    void downloadsAvailableFile() throws Exception {
        UUID id = UUID.randomUUID();
        DeliverableRepository.DeliverableRecord record = new DeliverableRepository.DeliverableRecord(
                id, PROJECT, PERIOD, STUDENT, "trabalho.pdf", "application/pdf", 1, "a".repeat(64), 1,
                DeliverableStatus.AVAILABLE, "deliverables/test", NOW.minusSeconds(900), NOW.plusSeconds(1));
        when(repository.findForToken(any(), anyString(), anyString())).thenReturn(record);
        when(storage.read("deliverables/test")).thenReturn(new byte[] {7});

        assertThat(service.download(id, "token").content()).containsExactly(7);
    }
}
