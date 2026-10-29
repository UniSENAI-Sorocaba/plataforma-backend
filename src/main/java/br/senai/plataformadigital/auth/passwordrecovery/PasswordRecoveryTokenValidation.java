package br.senai.plataformadigital.auth.passwordrecovery;

import java.time.Instant;

/** Safe result of validation; it never contains the submitted token or subject. */
public record PasswordRecoveryTokenValidation(
        PasswordRecoveryTokenStatus status,
        Instant expiresAt) {

    public boolean isValid() {
        return status == PasswordRecoveryTokenStatus.VALID;
    }
}
