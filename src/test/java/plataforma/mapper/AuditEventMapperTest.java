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
        String password = "senha falsa com espaço";
        String token = "token fictício com espaço";
        String cpfPontuado = "123.456.789-09";
        String cpfSemPontuacao = "12345678909";
        String exceptionPayload = "mensagem fictícia da exceção";
        var mapper = new AuditEventMapper();
        var request = new RecordAuditEventRequest(
                "auth", "user_account", AuditOperation.INSERT, UUID.randomUUID(), null,
                Map.of("cpf", cpfSemPontuacao),
                Map.of(
                        "profile", Map.of(
                                "email", "aluno@exemplo.test",
                                "password", password,
                                "token", token,
                                "cpf", cpfPontuado,
                                "comment", "password=" + password),
                        "exception", exceptionPayload,
                        "stack_trace", "at fake.Handler.run(Handler.java:1): " + exceptionPayload,
                        "cause", new IllegalStateException(exceptionPayload)));

        var entity = mapper.toEntity(request, UUID.randomUUID(), Instant.now());
        var response = mapper.toResponse(entity);

        String persistedOldData = entity.getOldData().toString();
        String persistedData = entity.getNewData().toString();
        String responseOldData = response.oldData().toString();
        String responseData = response.newData().toString();

        assertThat(persistedOldData).contains("[REDACTED]").doesNotContain(cpfSemPontuacao);
        assertThat(responseOldData).contains("[REDACTED]").doesNotContain(cpfSemPontuacao);
        assertThat(persistedData)
                .contains("[REDACTED]", "[exception details omitted]")
                .doesNotContain(password, token, cpfPontuado, cpfSemPontuacao, exceptionPayload);
        assertThat(responseData)
                .contains("[REDACTED]", "[exception details omitted]")
                .doesNotContain(password, token, cpfPontuado, cpfSemPontuacao, exceptionPayload);
    }
}
