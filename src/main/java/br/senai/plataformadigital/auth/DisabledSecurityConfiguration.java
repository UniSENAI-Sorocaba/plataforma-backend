package br.senai.plataformadigital.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledSecurityConfiguration {
    @Bean
    SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
        // Mantém o modo local sem filtros de segurança nas rotas da aplicação.
        // Assim, uma identidade de teste/integração fornecida pelo servlet não é
        // substituída por um principal anônimo; o modo JWT usa outra cadeia.
        return http.securityMatcher("/__security-disabled__")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
