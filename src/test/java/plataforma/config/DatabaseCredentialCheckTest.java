package plataforma.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

/** BE-001: a missing required configuration must stop the application with a clear message. */
class DatabaseCredentialCheckTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(DatabaseCredentialCheck.class);

    @Test
    void startupFailsWhenThePasswordIsMissing() {
        runner.withBean(JdbcConnectionDetails.class, () -> details("")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("DB_PASSWORD");
        });
    }

    @Test
    void startupFailsWhenThePlaceholderWasNotResolved() {
        runner.withBean(JdbcConnectionDetails.class, () -> details("${DB_PASSWORD}")).run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void startupFailsWhenThePasswordIsNull() {
        runner.withBean(JdbcConnectionDetails.class, () -> details(null)).run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void startupSucceedsWhenThePasswordIsConfigured() {
        runner.withBean(JdbcConnectionDetails.class, () -> details("any-configured-value")).run(context ->
                assertThat(context).hasNotFailed());
    }

    @Test
    void startupSucceedsWithoutADataSource() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    /** The default profile must not carry a usable credential: it only reads DB_PASSWORD. */
    @Test
    void applicationYamlDoesNotVersionACredential() throws IOException {
        var sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"));
        Object password = sources.stream()
                .map(source -> source.getProperty("spring.datasource.password"))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow();
        assertThat(password).isEqualTo("${DB_PASSWORD:}");
    }

    private static JdbcConnectionDetails details(String password) {
        return new JdbcConnectionDetails() {
            @Override
            public String getJdbcUrl() {
                return "jdbc:postgresql://localhost:5432/plataforma";
            }

            @Override
            public String getUsername() {
                return "plataforma";
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
    }
}
