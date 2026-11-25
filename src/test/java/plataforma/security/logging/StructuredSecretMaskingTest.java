package plataforma.security.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

class StructuredSecretMaskingTest {

    @Test
    void logstashLineKeepsCorrelationIdAndMasksSecrets() {
        String secret = "sample-secret-value";
        var environment = new StandardEnvironment();
        var context = new LoggerContext();
        context.putObject(Environment.class.getName(), environment);
        var encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("logstash");
        encoder.start();

        try {
            var event = new LoggingEvent();
            event.setLevel(Level.INFO);
            event.setLoggerName("plataforma.security.logging.StructuredSecretMaskingTest");
            event.setMessage("request password=" + secret);
            event.setTimeStamp(Instant.parse("2026-10-29T12:00:00Z").toEpochMilli());
            event.setMDCPropertyMap(Map.of("correlation_id", "correlation-demo-42"));
            event.setThrowableProxy(new ThrowableProxy(new IllegalStateException("exception-payload-demo")));

            String line = new String(encoder.encode(event), StandardCharsets.UTF_8).strip();

            assertThat(line.split("\\R")).hasSize(1);
            assertThat(line).contains("\"correlation_id\":\"correlation-demo-42\"");
            assertThat(line).contains("\"message\":\"request password=[REDACTED]\"");
            assertThat(line).contains("[exception details omitted]");
            assertThat(line).doesNotContain(secret, "exception-payload-demo");
        }
        finally {
            encoder.stop();
            context.stop();
        }
    }
}
