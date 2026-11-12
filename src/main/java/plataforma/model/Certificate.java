package plataforma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Certificado de horas de extensão (no máximo um VALID por inscrição).
 *
 * <p>O conteúdo emitido (inscrição, horas e instante de emissão) não muda depois da emissão: o
 * {@code validationHash} é calculado sobre ele e qualquer divergência indica adulteração. A única
 * transição permitida é VALID → REVOKED.
 */
@Entity
@Table(schema = "extension", name = "certificate")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Uma inscrição pode ter vários certificados ao longo do tempo (ex.: revogado e emitido de novo),
     * mas só um VALID por vez. Índice proposto para a DB-013:
     * {@code create unique index on extension.certificate (enrollment_id) where status = 'VALID'}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;

    @Column(nullable = false, updatable = false)
    private int hours;

    /** SHA-256 (hex) do conteúdo canônico do certificado. */
    @Column(name = "validation_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String validationHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CertificateStatus status = CertificateStatus.VALID;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Motivo interno da revogação; não é exposto na validação pública. */
    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    protected Certificate() {
        // exigido pela JPA
    }

    private Certificate(Enrollment enrollment, int hours, Instant issuedAt, String validationHash) {
        this.enrollment = Objects.requireNonNull(enrollment, "enrollment");
        this.hours = hours;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.validationHash = Objects.requireNonNull(validationHash, "validationHash");
    }

    public static Certificate issue(Enrollment enrollment, int hours, Instant issuedAt, String validationHash) {
        return new Certificate(enrollment, hours, issuedAt, validationHash);
    }

    /** Revoga o certificado. Chamar em um certificado já revogado é erro de estado. */
    public void revoke(Instant when, String reason) {
        if (status == CertificateStatus.REVOKED) {
            throw new IllegalStateException("Certificado já revogado");
        }
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = Objects.requireNonNull(when, "when");
        this.revocationReason = Objects.requireNonNull(reason, "reason");
    }

    public UUID getId() {
        return id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public int getHours() {
        return hours;
    }

    public String getValidationHash() {
        return validationHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }
}
