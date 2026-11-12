package plataforma.model;

/** Situação de um certificado emitido. Revogar não apaga o registro: a validação pública passa a indicar REVOKED. */
public enum CertificateStatus {
    VALID,
    REVOKED
}
