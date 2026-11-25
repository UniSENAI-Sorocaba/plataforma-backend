package plataforma.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

class CorrelationPropagationConfigurationTest {
    private final CorrelationPropagationConfiguration configuration = new CorrelationPropagationConfiguration();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void copiesCorrelationToDecoratedTasks() {
        MDC.put("correlation_id", "async-42");
        AtomicReference<String> observedCorrelation = new AtomicReference<>();

        Runnable decorated = configuration.correlationTaskDecorator()
                .decorate(() -> observedCorrelation.set(MDC.get("correlation_id")));
        MDC.clear();
        decorated.run();

        assertThat(observedCorrelation).hasValue("async-42");
        assertThat(MDC.get("correlation_id")).isNull();
    }

    @Test
    void addsCorrelationHeaderToOutgoingRestTemplateRequests() {
        RestTemplate restTemplate = new RestTemplate();
        configuration.correlationRestTemplateCustomizer().customize(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MDC.put("correlation_id", "http-42");
        server.expect(requestTo("https://example.test/internal"))
                .andExpect(header(CorrelationIdFilter.HEADER, "http-42"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertThat(restTemplate.getForObject("https://example.test/internal", String.class)).isEqualTo("ok");
        server.verify();
    }
}
