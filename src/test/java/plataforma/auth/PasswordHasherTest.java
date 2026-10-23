package plataforma.auth;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import plataforma.dto.UserResponse;
import plataforma.model.UserAccount;
import plataforma.security.logging.SecretMaskingConverter;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private static final int MEMORY_KIB = 19_456;
    private Argon2Properties properties;
    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        properties = new Argon2Properties();
        hasher = new PasswordHasher(encoder(2), properties);
    }

    @Test
    void hashUsesDistinctSaltsAndVerifiesWithoutReturningThePassword() {
        String password = randomPassword();

        String firstHash = hasher.hash(password);
        String secondHash = hasher.hash(password);

        assertThat(firstHash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$")).isTrue();
        assertThat(firstHash.equals(secondHash)).isFalse();
        assertThat(firstHash.contains(password)).isFalse();
        assertThat(hasher.verify(password, firstHash)).isTrue();
        assertThat(hasher.verify(randomPassword(), firstHash)).isFalse();
        assertThat(hasher.needsRehash(firstHash)).isFalse();
    }

    @Test
    void malformedHashesFailClosedWithoutExposingEncoderErrors() {
        String randomInput = randomPassword();

        assertThat(hasher.verify(randomInput, "not-a-hash")).isFalse();
        assertThat(hasher.verify(randomInput, "$argon2id$v=19$m=2147483647,t=2,p=1$YQ$YQ")).isFalse();
        assertThat(hasher.verify(randomInput, "$argon2id$v=19$m=19456,t=2,p=1$%%%$%%%" )).isFalse();
        assertThat(hasher.needsRehash("not-a-hash")).isTrue();
    }

    @Test
    void weakerSupportedHashNeedsTransparentUpgrade() {
        Argon2PasswordEncoder previousEncoder = encoder(1);
        String priorHash = previousEncoder.encode(randomPassword());

        assertThat(hasher.needsRehash(priorHash)).isTrue();
    }

    @Test
    void capturedLogOutputRedactsSecretsAndOmitsExceptionMessages() {
        String token = randomPassword();
        String password = randomPassword();
        var context = new LoggerContext();
        context.start();
        var output = new StringBuilder();
        AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
            private final SecretMaskingConverter converter = new SecretMaskingConverter();

            @Override
            protected void append(ILoggingEvent event) {
                output.append(converter.convert(event)).append(System.lineSeparator());
            }
        };
        appender.setContext(context);
        appender.start();
        Logger logger = context.getLogger("password-hasher-security-test");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);

        try {
            logger.info("request token={}", token);
            logger.warn("credential password={}", password);
            logger.error("operation failed", new IllegalStateException(password));
            String capturedOutput = output.toString();

            assertThat(capturedOutput.contains(token)).isFalse();
            assertThat(capturedOutput.contains(password)).isFalse();
            assertThat(capturedOutput.contains("[REDACTED]")).isTrue();
            assertThat(capturedOutput.contains("[exception message omitted]")).isTrue();
        }
        finally {
            logger.detachAppender(appender);
            context.stop();
        }
    }

    @Test
    void userPersistenceModelDoesNotStorePlaintextCredentialFields() {
        boolean plaintextCredentialFieldExists = Arrays.stream(UserAccount.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .anyMatch(field -> field.getName().equalsIgnoreCase("password")
                        || field.getName().equalsIgnoreCase("senha"));
        boolean credentialResponseFieldExists = Arrays.stream(UserResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase(java.util.Locale.ROOT).contains("password")
                        || component.getName().toLowerCase(java.util.Locale.ROOT).contains("senha")
                        || component.getName().toLowerCase(java.util.Locale.ROOT).contains("hash"));

        assertThat(plaintextCredentialFieldExists).isFalse();
        assertThat(credentialResponseFieldExists).isFalse();
    }

    private static Argon2PasswordEncoder encoder(int iterations) {
        return new Argon2PasswordEncoder(16, 32, 1, MEMORY_KIB, iterations);
    }

    private static String randomPassword() {
        return UUID.randomUUID().toString();
    }
}
