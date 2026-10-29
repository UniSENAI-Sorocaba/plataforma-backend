package br.senai.plataformadigital.auth.passwordrecovery;

import java.util.Objects;

/** Coordinates neutral recovery requests without exposing account existence. */
public final class PasswordRecoveryRequestService {
    private final PasswordRecoveryAccountLookup accountLookup;
    private final PasswordRecoveryTokenService tokenService;
    private final PasswordRecoveryTokenDelivery tokenDelivery;

    public PasswordRecoveryRequestService(
            PasswordRecoveryAccountLookup accountLookup,
            PasswordRecoveryTokenService tokenService,
            PasswordRecoveryTokenDelivery tokenDelivery) {
        this.accountLookup = Objects.requireNonNull(accountLookup, "accountLookup must not be null");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.tokenDelivery = Objects.requireNonNull(tokenDelivery, "tokenDelivery must not be null");
    }

    /**
     * Requests recovery for a subject. The method has no result that can
     * distinguish a known subject from an unknown one.
     */
    public void request(String subject) {
        String normalizedSubject = PasswordRecoveryTokenService.normalizeSubject(subject);
        if (!accountLookup.exists(normalizedSubject)) {
            return;
        }
        IssuedPasswordRecoveryToken issued = tokenService.issueForSubject(normalizedSubject);
        tokenDelivery.deliver(normalizedSubject, issued.rawToken(), issued.expiresAt());
    }
}
