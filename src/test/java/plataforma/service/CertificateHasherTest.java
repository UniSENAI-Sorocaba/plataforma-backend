package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CertificateHasherTest {

    private static final UUID ENROLLMENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STUDENT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NOTICE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant ISSUED_AT = Instant.parse("2026-11-09T15:00:00Z");

    private final CertificateHasher hasher = new CertificateHasher();

    @Test
    void mesmasEntradasGeramSempreOMesmoHash() {
        var first = hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT);
        var second = hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT);

        assertThat(first).isEqualTo(second).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void hashFixoParaConteudoCanonicoConhecido() {
        // Trava o formato canônico v1: se mudar, certificados já emitidos deixam de validar.
        assertThat(CertificateHasher.canonical(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT)).isEqualTo(
                "plataforma-certificate:v1|enrollment=11111111-1111-1111-1111-111111111111"
                        + "|student=22222222-2222-2222-2222-222222222222"
                        + "|notice=33333333-3333-3333-3333-333333333333|hours=40|issuedAt=2026-11-09T15:00:00Z");
        assertThat(hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT))
                .isEqualTo("e69e7208068a71671b85545fa3dfb987efc93154ca473d846c57deb57402a466");
    }

    @Test
    void qualquerCampoAlteradoMudaOHash() {
        var original = hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT);

        assertThat(hasher.hash(UUID.randomUUID(), STUDENT, NOTICE, 40, ISSUED_AT)).isNotEqualTo(original);
        assertThat(hasher.hash(ENROLLMENT, UUID.randomUUID(), NOTICE, 40, ISSUED_AT)).isNotEqualTo(original);
        assertThat(hasher.hash(ENROLLMENT, STUDENT, UUID.randomUUID(), 40, ISSUED_AT)).isNotEqualTo(original);
        assertThat(hasher.hash(ENROLLMENT, STUDENT, NOTICE, 41, ISSUED_AT)).isNotEqualTo(original);
        assertThat(hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT.plusSeconds(1))).isNotEqualTo(original);
    }

    @Test
    void comparacaoAceitaMaiusculasERejeitaNuloOuDiferente() {
        var hash = hasher.hash(ENROLLMENT, STUDENT, NOTICE, 40, ISSUED_AT);

        assertThat(hasher.matches(hash, hash.toUpperCase())).isTrue();
        assertThat(hasher.matches(hash, null)).isFalse();
        assertThat(hasher.matches(hash, hash.substring(1))).isFalse();
        assertThat(hasher.matches(hash, "0".repeat(64))).isFalse();
    }
}
