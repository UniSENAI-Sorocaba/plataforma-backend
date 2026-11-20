package br.senai.plataformadigital.auth;

import java.time.Clock;
import java.time.Duration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.senai.plataformadigital.auth.passwordrecovery.InMemoryPasswordRecoveryTokenRepository;
import br.senai.plataformadigital.auth.passwordrecovery.JdbcPasswordRecoveryTokenRepository;
import br.senai.plataformadigital.auth.passwordrecovery.PasswordRecoveryTokenPolicy;
import br.senai.plataformadigital.auth.passwordrecovery.PasswordRecoveryTokenRepository;
import br.senai.plataformadigital.auth.passwordrecovery.PasswordRecoveryTokenService;

/** Activates the DB-005 adapter only when JWT mode and a datasource are configured. */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
class SessionPersistenceConfiguration {
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url")
    RefreshTokenRepository refreshTokenRepository(DataSource dataSource) {
        return new JdbcRefreshTokenRepository(dataSource);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    UserAccountRepository userAccountRepository(DataSource dataSource) {
        return new JdbcUserAccountRepository(dataSource);
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

    @Bean
    @ConditionalOnMissingBean(value = {PasswordRecoveryTokenRepository.class, DataSource.class})
    PasswordRecoveryTokenRepository passwordRecoveryTokenRepository() {
        return new InMemoryPasswordRecoveryTokenRepository();
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(PasswordRecoveryTokenRepository.class)
    PasswordRecoveryTokenRepository jdbcPasswordRecoveryTokenRepository(DataSource dataSource) {
        return new JdbcPasswordRecoveryTokenRepository(dataSource);
    }

    @Bean
    PasswordRecoveryTokenService passwordRecoveryTokenService(
            PasswordRecoveryTokenRepository repository,
            Clock clock,
            @Value("${security.password-recovery.lifetime:30m}") Duration lifetime) {
        return new PasswordRecoveryTokenService(repository, clock, PasswordRecoveryTokenPolicy.withLifetime(lifetime));
    }
}
