package plataforma.security.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import plataforma.security.SecretRedactor;

/** Redacts string values while leaving Spring Boot's structured log format intact. */
public final class SecretMaskingJsonCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        JsonWriter.ValueProcessor<Object> processor = (path, value) -> {
            if (!(value instanceof String text)) {
                return value;
            }
            if (path.toUnescapedString().endsWith("stack_trace")) {
                return "[exception details omitted]";
            }
            return SecretRedactor.redactText(text);
        };
        members.applyingValueProcessor(processor);
    }
}
