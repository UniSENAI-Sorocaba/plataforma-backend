package plataforma.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/** Preserves request correlation in executor tasks and outgoing RestTemplate calls. */
@Configuration(proxyBeanMethods = false)
public class CorrelationPropagationConfiguration {
    @Bean
    TaskDecorator correlationTaskDecorator() {
        return runnable -> {
            Map<String, String> parentContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                try {
                    if (parentContext == null) MDC.clear(); else MDC.setContextMap(parentContext);
                    runnable.run();
                } finally {
                    if (previousContext == null) MDC.clear(); else MDC.setContextMap(previousContext);
                }
            };
        };
    }

    @Bean
    RestTemplateCustomizer correlationRestTemplateCustomizer() {
        return template -> template.getInterceptors().add((request, body, execution) -> {
            String correlationId = MDC.get("correlation_id");
            if (correlationId != null && !request.getHeaders().containsKey(CorrelationIdFilter.HEADER)) {
                request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
            }
            return execution.execute(request, body);
        });
    }
}
