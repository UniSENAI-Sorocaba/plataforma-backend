package plataforma.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import plataforma.dto.RecordAuditEventRequest;
import plataforma.model.AuditOperation;

class AuditEventMapperTest {

    @Test
    void redigeSegredosAntesDePersistirEDevolverEvento() {
        String credential = UUID.randomUUID().toString();
        var mapper = new AuditEventMapper();
        var request = new RecordAuditEventRequest(
                "auth", "user_account", AuditOperation.INSERT, UUID.randomUUID(), null,
                null, Map.of("profile", Map.of("email", "aluno@exemplo.test", "password", credential)));

        var entity = mapper.toEntity(request, UUID.randomUUID(), Instant.now());
        var response = mapper.toResponse(entity);

        assertThat(entity.getNewData().toString()).contains("[REDACTED]").doesNotContain(credential);
        assertThat(response.newData().toString()).contains("[REDACTED]").doesNotContain(credential);
    }
}
