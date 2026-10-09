package plataforma;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** PostgreSQL 16 real com as migrations do repositório de banco aplicadas na inicialização. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final String INIT_DIR = "/docker-entrypoint-initdb.d/";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("database/V0001__create_domain_schemas.sql"),
                        INIT_DIR + "V0001__create_domain_schemas.sql")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("database/V0002__create_audit_event.sql"),
                        INIT_DIR + "V0002__create_audit_event.sql");
    }
}
