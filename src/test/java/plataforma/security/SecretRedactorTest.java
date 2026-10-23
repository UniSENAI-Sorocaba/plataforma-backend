package plataforma.security;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import plataforma.security.logging.SecretMaskingConverter;

class SecretRedactorTest {

    @Test
    void redactsSecretFieldsAndBearerValuesFromText() {
        String password = UUID.randomUUID().toString();
        String bearer = UUID.randomUUID().toString();
        String key = UUID.randomUUID().toString();
        String safe = SecretRedactor.redactText(
                "{\"password\":\"" + password + "\", senha=" + password + ", Authorization: Bearer "
                        + bearer + ", api_key='" + key + "'}");

        assertThat(safe).doesNotContain(password, bearer, key)
                .contains("[REDACTED]");
    }

    @Test
    void redactsNestedAuditDataAndSensitiveKeys() {
        String token = UUID.randomUUID().toString();
        String cookieValue = UUID.randomUUID().toString();
        var safe = SecretRedactor.redactMap(Map.of(
                "status", "active",
                "refresh_token", token,
                "details", List.of(Map.of("email", "person@example.test", "cookie", cookieValue))));

        assertThat(safe).containsEntry("status", "active").containsEntry("refresh_token", "[REDACTED]");
        assertThat(safe.get("details").toString()).doesNotContain(token, cookieValue)
                .contains("[REDACTED]");
    }

    @Test
    void logConverterRedactsFormattedMessageAndThrowable() {
        String token = UUID.randomUUID().toString();
        var event = new LoggingEvent();
        event.setMessage("request token=" + token);
        event.setThrowableProxy(new ThrowableProxy(new IllegalStateException("password=" + token)));

        String safe = new SecretMaskingConverter().convert(event);

        assertThat(safe).doesNotContain(token).contains("[REDACTED]");
    }
}
