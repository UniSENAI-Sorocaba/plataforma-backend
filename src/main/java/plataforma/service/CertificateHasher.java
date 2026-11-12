package plataforma.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Calcula o hash SHA-256 do conteúdo canônico de um certificado.
 *
 * <p>O conteúdo é uma linha de texto com versão e campos em ordem fixa, então as mesmas entradas
 * sempre geram o mesmo hash. Qualquer mudança em inscrição, aluno, edital, horas ou instante de
 * emissão gera um hash diferente, o que permite detectar adulteração.
 *
 * <p>Isto não é assinatura digital: o tipo de assinatura ainda depende de decisão do PO (SPEC-EXT-003).
 */
@Component
public class CertificateHasher {

    static final String VERSION = "plataforma-certificate:v1";

    public String hash(UUID enrollmentId, UUID studentId, UUID noticeId, int hours, Instant issuedAt) {
        return HexFormat.of().formatHex(sha256(canonical(enrollmentId, studentId, noticeId, hours, issuedAt)));
    }

    /** Compara em tempo constante para não vazar, pelo tempo de resposta, quantos caracteres coincidem. */
    public boolean matches(String expected, String candidate) {
        if (expected == null || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                candidate.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    static String canonical(UUID enrollmentId, UUID studentId, UUID noticeId, int hours, Instant issuedAt) {
        return String.join("|",
                VERSION,
                "enrollment=" + Objects.requireNonNull(enrollmentId, "enrollmentId"),
                "student=" + Objects.requireNonNull(studentId, "studentId"),
                "notice=" + Objects.requireNonNull(noticeId, "noticeId"),
                "hours=" + hours,
                "issuedAt=" + Objects.requireNonNull(issuedAt, "issuedAt"));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
        }
    }
}
