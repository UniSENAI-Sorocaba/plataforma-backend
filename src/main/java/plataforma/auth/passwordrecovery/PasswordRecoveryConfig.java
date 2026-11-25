package plataforma.auth.passwordrecovery;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class PasswordRecoveryConfig {

    @Bean
    @Profile("test")
    PasswordRecoveryTokenRepository inMemoryPasswordRecoveryTokenRepository() {
        return new InMemoryPasswordRecoveryTokenRepository();
    }

    @Bean
    @Profile("!test")
    PasswordRecoveryTokenRepository passwordRecoveryTokenRepository(
            PasswordRecoveryTokenJpaRepository repository) {
        return new JpaPasswordRecoveryTokenRepository(repository);
    }

    @Bean
    PasswordRecoveryTokenService passwordRecoveryTokenService(
            PasswordRecoveryTokenRepository repo,
            Clock clock,
            @Value("${auth.password-recovery.lifetime:PT20M}") Duration validade) {
        return new PasswordRecoveryTokenService(repo, clock,
                PasswordRecoveryTokenPolicy.withLifetime(validade));
    }

    @Bean(name = "passwordRecoveryDeliveryExecutor")
    Executor passwordRecoveryDeliveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-recovery-");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnBean({PasswordRecoveryAccountLookup.class, PasswordRecoveryTokenDelivery.class})
    PasswordRecoveryRequestService passwordRecoveryRequestService(
            PasswordRecoveryAccountLookup accountLookup,
            PasswordRecoveryTokenService tokenService,
            PasswordRecoveryTokenDelivery tokenDelivery,
            @Qualifier("passwordRecoveryDeliveryExecutor") Executor deliveryExecutor) {
        return new PasswordRecoveryRequestService(accountLookup, tokenService, tokenDelivery, deliveryExecutor);
    }

    @Bean
    @ConditionalOnBean({PasswordHasher.class, PasswordRecoveryAccountStore.class, PasswordRecoverySessionRevoker.class})
    PasswordResetService passwordResetService(
            PasswordRecoveryTokenService tokenService,
            PasswordHasher passwordHasher,
            PasswordRecoveryAccountStore accountStore,
            PasswordRecoverySessionRevoker sessionRevoker) {
        return new PasswordResetService(tokenService, passwordHasher, accountStore, sessionRevoker);
    }
}
