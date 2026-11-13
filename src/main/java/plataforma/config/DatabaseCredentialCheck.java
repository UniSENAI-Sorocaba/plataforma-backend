package plataforma.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.stereotype.Component;

/**
 * Fails the startup when the database credential was not configured, so the problem shows up as a
 * clear message instead of an authentication error from the first connection.
 *
 * <p>The check runs when the {@link JdbcConnectionDetails} bean is created. That bean always exists
 * before the {@code DataSource} and the JPA {@code EntityManagerFactory} try to connect, which a
 * {@code @PostConstruct} in an ordinary bean would not guarantee. It validates the effective
 * connection details, so credentials supplied by Testcontainers count, and it does nothing when no
 * {@code DataSource} is configured. A placeholder that Spring could not resolve (for example
 * {@code ${DB_PASSWORD}}) is bound as plain text, which is why it is rejected too.
 */
@Component
public class DatabaseCredentialCheck implements BeanPostProcessor {

    static final String MISSING_MESSAGE =
            "Required configuration missing: set DB_PASSWORD with the database password (see .env.example).";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof JdbcConnectionDetails details) {
            String password = details.getPassword();
            if (password == null || password.isBlank() || password.startsWith("${")) {
                throw new IllegalStateException(MISSING_MESSAGE);
            }
        }
        return bean;
    }
}
