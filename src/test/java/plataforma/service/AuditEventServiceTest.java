package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import plataforma.dto.RecordAuditEventRequest;
import plataforma.mapper.AuditEventMapper;
import plataforma.model.AuditEvent;
import plataforma.model.AuditOperation;
import plataforma.repository.AuditEventRepository;
import plataforma.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");

    private AuditEventRepository repository;
    private AuditEventService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditEventRepository.class);
        service = new AuditEventService(repository, new AuditEventMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordGeraIdEInstanteNaAplicacao() {
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var actorId = UUID.randomUUID();

        var response = service.record(new RecordAuditEventRequest(
                "auth", "user_account", AuditOperation.INSERT, UUID.randomUUID(), actorId,
                null, Map.of("email", "aluno@exemplo.test")));

        assertThat(response.id()).isNotNull();
        assertThat(response.occurredAt()).isEqualTo(NOW);
        assertThat(response.actorId()).isEqualTo(actorId);
        assertThat(response.operation()).isEqualTo(AuditOperation.INSERT);
    }

    @Test
    void recordRedigeSegredosDeDadosAninhadosAntesDePersistir() {
        String credential = UUID.randomUUID().toString();
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.record(new RecordAuditEventRequest(
                "auth", "user_account", AuditOperation.INSERT, UUID.randomUUID(), null,
                null, Map.of("profile", Map.of("email", "aluno@exemplo.test", "password", credential))));

        String responseData = response.newData().toString();
        assertThat(responseData.contains("[REDACTED]")).isTrue();
        assertThat(responseData.contains(credential)).isFalse();
    }

    @Test
    void findByIdLancaNotFoundQuandoNaoExiste() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
