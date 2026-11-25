package plataforma.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Argon2Properties.class)
public class PasswordHashConfiguration {

    @Bean
    PasswordHasher passwordHasher(Argon2Properties properties) {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                properties.getSaltLength(),
                properties.getHashLength(),
                properties.getParallelism(),
                properties.getMemoryKib(),
                properties.getIterations());
        return new PasswordHasher(encoder, properties);
    }
}
