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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void configuredParametersAndStoredHashBoundsRejectBothSidesOfEveryLimit() {
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isTrue();
        properties.getHashSanity().setMinMemoryKib(19_457);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.getHashSanity().setMinMemoryKib(8);
        properties.setMemoryKib(262_145);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setMemoryKib(19_456);
        properties.getHashSanity().setMaxMemoryKib(19_455);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();

        properties.getHashSanity().setMaxMemoryKib(262_144);
        properties.setIterations(1);
        properties.getHashSanity().setMinIterations(2);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.getHashSanity().setMinIterations(1);
        properties.setIterations(11);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setIterations(2);
        properties.setParallelism(0);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setParallelism(17);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setParallelism(1);
        properties.setSaltLength(15);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setSaltLength(129);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.setSaltLength(16);
        properties.getHashSanity().setMinHashLength(33);
        properties.setHashLength(31);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();
        properties.getHashSanity().setMinHashLength(16);
        properties.setHashLength(129);
        assertThat(properties.isConfiguredWithinHashSanityLimits()).isFalse();

        var limits = new Argon2Properties.HashSanityLimits();
        assertThat(limits.isValid()).isTrue();
        limits.setMinIterations(11);
        assertThat(limits.isValid()).isFalse();
        limits.setMinIterations(1);
        limits.setMaxIterations(0);
        assertThat(limits.isValid()).isFalse();
        limits.setMaxIterations(10);
        limits.setMinParallelism(17);
        assertThat(limits.isValid()).isFalse();
        limits.setMinParallelism(1);
        limits.setMaxParallelism(0);
        assertThat(limits.isValid()).isFalse();
        limits.setMaxParallelism(16);
        limits.setMinSaltLength(129);
        assertThat(limits.isValid()).isFalse();
        limits.setMinSaltLength(16);
        limits.setMaxSaltLength(15);
        assertThat(limits.isValid()).isFalse();
        limits.setMaxSaltLength(128);
        limits.setMinHashLength(129);
        assertThat(limits.isValid()).isFalse();
        limits.setMinHashLength(16);
        limits.setMaxHashLength(15);
        assertThat(limits.isValid()).isFalse();

        assertThat(hasher.verify(null, "malformed")).isFalse();
        assertThat(hasher.needsRehash(null)).isTrue();
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEncodedHashLengthAndEveryParsedParameterBoundary() {
        String password = randomPassword();
        String salt = base64Bytes(16);
        String hash = base64Bytes(32);
        for (String malformed : Arrays.asList(
                "x".repeat(513),
                encoded(7, 2, 1, salt, hash),
                encoded(19_456, 0, 1, salt, hash),
                encoded(19_456, 2, 0, salt, hash),
                encoded(8, 2, 2, salt, hash),
                encoded(19_456, 2, 1, base64Bytes(8), hash),
                encoded(19_456, 2, 1, salt, base64Bytes(8)),
                encoded(19_456, 2, 1, base64Bytes(130), hash),
                encoded(19_456, 2, 1, salt, base64Bytes(130)),
                "$argon2id$v=19$m=999999999999999999999,t=2,p=1$" + salt + "$" + hash)) {
            assertThat(hasher.verify(password, malformed)).isFalse();
            assertThat(hasher.needsRehash(malformed)).isTrue();
        }

        assertThat(hasher.needsRehash(encoder(19_456, 2, 2, 16, 32).encode(password))).isTrue();
        assertThat(hasher.needsRehash(encoder(19_456, 2, 1, 24, 32).encode(password))).isTrue();
        assertThat(hasher.needsRehash(encoder(19_456, 2, 1, 16, 40).encode(password))).isTrue();
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
