package br.senai.plataformadigital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação gerada pelo Swagger UI. Pela ADR-009, a documentação gerada
 * não substitui o contrato OpenAPI versionado.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI plataformaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Plataforma Digital — Backend")
                .description("API do monólito modular da Plataforma Digital")
                .version("v1"));
    }
}
