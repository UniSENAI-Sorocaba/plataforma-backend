package plataforma.auth;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

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

        assertThat(firstHash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(firstHash).doesNotContain(password);
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

    private static Argon2PasswordEncoder encoder(int iterations) {
        return new Argon2PasswordEncoder(16, 32, 1, MEMORY_KIB, iterations);
    }

    private static String randomPassword() {
        return UUID.randomUUID().toString();
    }
}
