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

        assertThat(safe.contains(password)).isFalse();
        assertThat(safe.contains(bearer)).isFalse();
        assertThat(safe.contains(key)).isFalse();
        assertThat(safe.contains("[REDACTED]")).isTrue();
    }

    @Test
    void redactsNestedAuditDataAndSensitiveKeys() {
        String token = UUID.randomUUID().toString();
        String cookieValue = UUID.randomUUID().toString();
        var safe = SecretRedactor.redactMap(Map.of(
                "status", "active",
                "refresh_token", token,
                "details", List.of(Map.of("email", "person@example.test", "cookie", cookieValue))));

        assertThat(safe.get("status").equals("active")).isTrue();
        assertThat(safe.get("refresh_token").equals("[REDACTED]")).isTrue();
        String safeDetails = safe.get("details").toString();
        assertThat(safeDetails.contains(token)).isFalse();
        assertThat(safeDetails.contains(cookieValue)).isFalse();
        assertThat(safeDetails.contains("[REDACTED]")).isTrue();
    }

    @Test
    void redactsCpfAndOmitsStructuredExceptionPayloads() {
        String exceptionPayload = "fictional exception details";
        var safe = SecretRedactor.redactMap(Map.of(
                "cpf", "123.456.789-09",
                "exceptionMessage", exceptionPayload,
                "payload", new IllegalStateException(exceptionPayload),
                "nested", List.of(Map.of("stack_trace", "trace " + exceptionPayload))));

        assertThat(safe.get("cpf")).isEqualTo("[REDACTED]");
        assertThat(safe.get("exceptionMessage")).isEqualTo("[exception details omitted]");
        assertThat(safe.get("payload")).isEqualTo("[exception details omitted]");
        assertThat(safe.get("nested").toString()).contains("[exception details omitted]").doesNotContain(exceptionPayload);
    }

    @Test
    void logConverterRedactsFormattedMessageAndThrowable() {
        String token = UUID.randomUUID().toString();
        var event = new LoggingEvent();
        event.setMessage("request token=" + token);
        event.setThrowableProxy(new ThrowableProxy(new IllegalStateException("password=" + token)));

        String safe = new SecretMaskingConverter().convert(event);

        assertThat(safe.contains(token)).isFalse();
        assertThat(safe.contains("[REDACTED]")).isTrue();
    }

    @Test
    void redactsCompleteAuthorizationAndPasswordValuesIncludingSpaces() {
        assertThat(SecretRedactor.redactText("Authorization: Basic abc123"))
                .isEqualTo("Authorization: [REDACTED]");
        assertThat(SecretRedactor.redactText("Authorization: Bearer <token>"))
                .isEqualTo("Authorization: [REDACTED]");
        assertThat(SecretRedactor.redactText("password=minha senha"))
                .isEqualTo("password=[REDACTED]");
        assertThat(SecretRedactor.redactText("texto seguro password=minha senha&mode=read fim"))
                .isEqualTo("texto seguro password=[REDACTED]&mode=read fim");
    }

    @Test
    void redactsSecretKeyVariantsCaseInsensitivelyAndPreservesSeparators() {
        String source = "password: alpha beta; SeNhA=gamma delta, \"password\":\"valor com espaço\" "
                + "TOKEN=one two&access_token=three four refresh_token:five six api_key=seven eight "
                + "secret=nine ten cookie=eleven twelve Set-Cookie:thirteen fourteen";
        String result = SecretRedactor.redactText(source);

        assertThat(result).isEqualTo("password: [REDACTED]; SeNhA=[REDACTED], \"password\":\"[REDACTED]\" "
                + "TOKEN=[REDACTED]&access_token=[REDACTED] refresh_token:[REDACTED] api_key=[REDACTED] "
                + "secret=[REDACTED] cookie=[REDACTED] Set-Cookie:[REDACTED]");
    }

    @Test
    void redactsMultipleSecretsOnSeparateLinesWithoutConsumingFollowingText() {
        assertThat(SecretRedactor.redactText("password=first value\ntoken=second value\nstatus=healthy"))
                .isEqualTo("password=[REDACTED]\ntoken=[REDACTED]\nstatus=healthy");
    }

    @Test
    void redactsValidCpfWithAndWithoutPunctuationButLeavesOtherNumbersAlone() {
        assertThat(SecretRedactor.redactText("cpf 123.456.789-09 and 12345678909"))
                .isEqualTo("cpf [REDACTED] and [REDACTED]");
        assertThat(SecretRedactor.redactText("id=123 timestamp=1730304000 epoch=1730304000000 invalid=12345678900"))
                .isEqualTo("id=123 timestamp=1730304000 epoch=1730304000000 invalid=12345678900");
    }

    @Test
    void leavesSafeTextUnchangedAndHandlesLongInput() {
        String safe = "status=healthy id=42 timestamp=1730304000";
        assertThat(SecretRedactor.redactText(safe)).isEqualTo(safe);

        String longInput = "password=" + "x".repeat(200_000) + "&status=ok";
        String result = SecretRedactor.redactText(longInput);
        assertThat(result).isEqualTo("password=[REDACTED]&status=ok");
    }
}
