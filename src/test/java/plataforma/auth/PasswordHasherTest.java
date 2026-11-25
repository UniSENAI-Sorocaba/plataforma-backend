package plataforma.auth;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import plataforma.dto.UserResponse;
import plataforma.model.UserAccount;
import plataforma.security.logging.SecretMaskingConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
        assertThatCode(() -> hasher.verify(randomInput, "$argon2id$v=19$m=19456,t=2,p=1$%%%$%%%"))
                .doesNotThrowAnyException();
        assertThat(hasher.needsRehash("not-a-hash")).isTrue();
    }

    @Test
    void weakerSupportedHashNeedsTransparentUpgrade() {
        String password = randomPassword();
        Argon2PasswordEncoder previousEncoder = encoder(19_456, 1, 1, 16, 16);
        String priorHash = previousEncoder.encode(password);

        assertThat(hasher.verify(password, priorHash)).isTrue();
        assertThat(hasher.verify(randomPassword(), priorHash)).isFalse();
        assertThat(hasher.needsRehash(priorHash)).isTrue();
    }

    @Test
    void strongerSupportedHashVerifiesAndRequestsRehashToCurrentConfiguration() {
        String password = randomPassword();
        Argon2PasswordEncoder strongerEncoder = encoder(24_576, 3, 2, 16, 32);
        String strongerHash = strongerEncoder.encode(password);

        assertThat(hasher.verify(password, strongerHash)).isTrue();
        assertThat(hasher.verify(randomPassword(), strongerHash)).isFalse();
        assertThat(hasher.needsRehash(strongerHash)).isTrue();
    }

    @Test
    void rejectsArgonParametersAndEncodedFieldsOutsideSanityLimits() {
        String password = randomPassword();
        String validSalt = base64Bytes(16);
        String validHash = base64Bytes(32);
        String tooMuchMemory = encoded(262_145, 2, 1, validSalt, validHash);
        String tooManyIterations = encoded(19_456, 11, 1, validSalt, validHash);
        String tooMuchParallelism = encoded(19_456, 2, 17, validSalt, validHash);
        String oversizedSalt = encoded(19_456, 2, 1, base64Bytes(129), validHash);
        String oversizedHash = encoded(19_456, 2, 1, validSalt, base64Bytes(129));

        for (String absurdHash : Arrays.asList(
                tooMuchMemory, tooManyIterations, tooMuchParallelism, oversizedSalt, oversizedHash)) {
            assertThatCode(() -> hasher.verify(password, absurdHash)).doesNotThrowAnyException();
            assertThat(hasher.verify(password, absurdHash)).isFalse();
            assertThat(hasher.needsRehash(absurdHash)).isTrue();
        }
    }

    @Test
    void configuredSanityLimitsAreIndependentAndCanBeTightened() {
        String password = randomPassword();
        Argon2PasswordEncoder strongerEncoder = encoder(20_480, 2, 1, 16, 32);
        String strongerHash = strongerEncoder.encode(password);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isTrue();
        properties.getHashSanity().setMaxMemoryKib(20_000);

        assertThat(properties.isConfiguredWithinHashSanityLimits()).isTrue();
        assertThat(hasher.verify(password, strongerHash)).isFalse();
        assertThat(hasher.needsRehash(strongerHash)).isTrue();

        properties.setMemoryKib(20_001);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
    }

    @Test
    void sanityLimitsCanBeOverriddenThroughSpringConfigurationProperties() {
        var source = new MapConfigurationPropertySource(java.util.Map.of(
                "platform.security.password.argon2.hash-sanity.min-memory-kib", "16",
                "platform.security.password.argon2.hash-sanity.max-memory-kib", "131072",
                "platform.security.password.argon2.hash-sanity.max-iterations", "8"));

        Argon2Properties bound = new Binder(source)
                .bind("platform.security.password.argon2", Bindable.of(Argon2Properties.class))
                .orElseThrow(() -> new AssertionError("Argon2 properties were not bound"));

        assertThat(bound.getHashSanity().getMinMemoryKib()).isEqualTo(16);
        assertThat(bound.getHashSanity().getMaxMemoryKib()).isEqualTo(131_072);
        assertThat(bound.getHashSanity().getMaxIterations()).isEqualTo(8);
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
        return encoder(MEMORY_KIB, iterations, 1, 16, 32);
    }

    private static Argon2PasswordEncoder encoder(int memoryKib, int iterations, int parallelism,
            int saltLength, int hashLength) {
        return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKib, iterations);
    }

    private static String encoded(int memoryKib, int iterations, int parallelism, String salt, String hash) {
        return "$argon2id$v=19$m=" + memoryKib + ",t=" + iterations + ",p=" + parallelism + "$" + salt + "$" + hash;
    }

    private static String base64Bytes(int length) {
        return Base64.getEncoder().encodeToString(new byte[length]);
    }

    private static String randomPassword() {
        return UUID.randomUUID().toString();
    }
}
