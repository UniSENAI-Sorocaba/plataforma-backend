package plataforma.security.metrics;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import plataforma.security.SecretRedactor;

/** Removes sensitive metric dimensions and redacts recognized secret values in remaining tags. */
@Configuration(proxyBeanMethods = false)
public class SecretMetricTagFilter {

    private static final Set<String> SENSITIVE_TAG_PARTS = Set.of(
            "password", "senha", "token", "authorization", "secret", "apikey", "cookie", "cpf", "taxid",
            "document", "credential");

    @Bean
    MeterFilter secretMetricTagMeterFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                var safeTags = new ArrayList<Tag>();
                for (Tag tag : id.getTags()) {
                    String key = tag.getKey().toLowerCase(Locale.ROOT);
                    if (!isSensitiveTag(key)) {
                        safeTags.add(Tag.of(tag.getKey(), SecretRedactor.redactText(tag.getValue())));
                    }
                }
                return id.replaceTags(safeTags);
            }
        };
    }

    private static boolean isSensitiveTag(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return SENSITIVE_TAG_PARTS.stream().anyMatch(normalized::contains);
    }
}
