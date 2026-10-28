package br.senai.plataformadigital.portal.impact;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@AutoConfiguration
public class ImpactConfiguration {
    @Bean
    @Profile("impact-local")
    @ConditionalOnMissingBean(ImpactRepository.class)
    ImpactRepository localImpactRepository() { return new InMemoryImpactRepository(); }

    @Bean
    @Profile("!impact-local")
    @ConditionalOnMissingBean(ImpactRepository.class)
    ImpactRepository unavailableImpactRepository() {
        // DB-015 fornece o schema/migrations. Sem adapter durável, não usar memória em produção.
        return new ImpactRepository() {
            public Optional<Impact> find(UUID id) { throw ImpactFailure.unavailable(); }
            public boolean create(Impact impact) { throw ImpactFailure.unavailable(); }
            public boolean replace(UUID id, long revision, Impact impact) { throw ImpactFailure.unavailable(); }
        };
    }

    @Bean
    @ConditionalOnMissingBean(ImpactPermissionProvider.class)
    ImpactPermissionProvider denyImpactPermissions() { return (actor, action, id) -> false; }

    @Bean
    @ConditionalOnMissingBean(ImpactAccess.class)
    ImpactAccess impactAccess(HttpServletRequest request, ImpactPermissionProvider permissions) {
        return new ImpactAccess() {
            public Optional<String> currentActor() {
                // BE-004 autentica e instala o Principal; não interpretar bearer/token aqui.
                return Optional.ofNullable(request.getUserPrincipal()).map(java.security.Principal::getName);
            }
            public boolean permits(String actor, Impact.Action action, UUID id) {
                return permissions.permits(actor, action, id);
            }
        };
    }

    @Bean
    ImpactService impactService(ImpactRepository repository, ImpactAccess access) {
        return new ImpactService(repository, access, Clock.systemUTC(), UUID::randomUUID);
    }
}
