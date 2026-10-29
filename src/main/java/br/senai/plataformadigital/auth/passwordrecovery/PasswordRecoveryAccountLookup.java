package br.senai.plataformadigital.auth.passwordrecovery;

/** Port used by the account module to avoid issuing tokens for unknown subjects. */
@FunctionalInterface
public interface PasswordRecoveryAccountLookup {
    boolean exists(String normalizedSubject);
}
