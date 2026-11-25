package plataforma.security.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecretMetricTagFilterTest {

    @Test
    void removesSensitiveTagKeysAndRedactsRecognizedValuesInOtherTags() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class))
                .withUserConfiguration(SecretMetricTagFilter.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MeterRegistry registry = context.getBean(MeterRegistry.class);
                    assertTagsAreSanitized(registry);
                    assertSensitiveMetricNamesAreRejected(registry);
                });
    }

    private static void assertTagsAreSanitized(MeterRegistry registry) {
        var counter = registry.counter("http.server.requests",
                "route", "/api/items/{id}",
                "password", "fictional-password",
                "cpf", "123.456.789-09",
                "account", "12345678909",
                "request", "token=sample-value",
                "exceptionMessage", "fictional exception message");

        assertThat(counter.getId().getTags())
                .extracting(Tag::getKey)
                .containsExactlyInAnyOrder("route", "account", "request");
        assertThat(counter.getId().getTag("route")).isEqualTo("/api/items/{id}");
        assertThat(counter.getId().getTag("account")).isEqualTo("[REDACTED]");
        assertThat(counter.getId().getTag("request")).isEqualTo("token=[REDACTED]");
    }

    private static void assertSensitiveMetricNamesAreRejected(MeterRegistry registry) {
        registry.counter("auth.password.attempts");
        registry.counter("login.12345678909");

        assertThat(registry.find("auth.password.attempts").counter()).isNull();
        assertThat(registry.find("login.12345678909").counter()).isNull();
    }
}
