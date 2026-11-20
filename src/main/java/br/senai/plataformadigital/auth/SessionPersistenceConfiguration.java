package br.senai.plataformadigital.auth;

import java.time.Clock;
import java.time.Duration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Activates the DB-005 adapter only when JWT mode and a datasource are configured. */
@Configuration
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
class SessionPersistenceConfiguration {
    @Bean
    @ConditionalOnBean(DataSource.class)
    RefreshTokenRepository refreshTokenRepository(DataSource dataSource) {
        return new JdbcRefreshTokenRepository(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTokenRepository.class)
    RefreshTokenRepository inMemoryRefreshTokenRepository() {
        return new InMemoryRefreshTokenRepository();
    }

    @Bean
    RefreshTokenService refreshTokenService(
            RefreshTokenRepository repository,
            Clock clock,
            @Value("${security.session.refresh-lifetime:30d}") Duration lifetime) {
        return new RefreshTokenService(repository, clock, lifetime);
    }
}
