package br.senai.plataformadigital.auth.web;

import br.senai.plataformadigital.auth.api.authorization.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration
public class AuthorizationConfiguration {
    @Bean @ConditionalOnMissingBean(AuthenticatedSubjectProvider.class)
    AuthenticatedSubjectProvider servletIdentity(HttpServletRequest request) {
        return () -> Optional.ofNullable(request.getUserPrincipal()).map(java.security.Principal::getName).map(AuthenticatedSubject::new);
    }

    @Bean @ConditionalOnMissingBean(AuthorizationPolicy.class)
    AuthorizationPolicy denyPolicy(ObjectProvider<AuthorizationPolicyContribution> contributions) {
        return (subject, request) -> {
            var matches = contributions.orderedStream().filter(policy -> policy.supports(request.action())).toList();
            return matches.size() == 1 && matches.getFirst().permits(subject, request);
        };
    }

    @Bean @ConditionalOnMissingBean(AuthorizationAudit.class)
    AuthorizationAudit auditPort() { return event -> { }; }

    @Bean
    AuthorizationService authorizationService(AuthenticatedSubjectProvider subjects, AuthorizationPolicy policy) {
        return new AuthorizationService(subjects, policy);
    }

    @Bean
    AuthorizationRoutes authorizationRoutes(ObjectMapper mapper) throws IOException {
        return AuthorizationRoutes.load(new PathMatchingResourcePatternResolver().getResources("classpath*:authorization/*.json"), mapper);
    }

    @Bean
    AuthorizationInterceptor authorizationInterceptor(AuthorizationRoutes routes, AuthorizationService service,
                                                      AuthorizationAudit audit, MeterRegistry registry, ObjectMapper mapper) {
        return new AuthorizationInterceptor(routes, service, audit, registry, mapper);
    }

    @Bean
    WebMvcConfigurer authorizationMvc(AuthorizationInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
        };
    }

    @Bean
    SmartInitializingSingleton authorizationCoverage(AuthorizationRoutes routes,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        return () -> routes.verify(mappings);
    }
}
