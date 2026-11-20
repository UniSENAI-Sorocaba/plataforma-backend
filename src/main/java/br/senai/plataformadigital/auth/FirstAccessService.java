package br.senai.plataformadigital.auth;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/** Activates an imported account exactly once using a CPF and a new Argon2id hash. */
@Service
@ConditionalOnBean(UserAccountRepository.class)
public class FirstAccessService {
    private final UserAccountRepository accounts;
    private final CredentialService credentials;

    public FirstAccessService(UserAccountRepository accounts, CredentialService credentials) {
        this.accounts = Objects.requireNonNull(accounts);
        this.credentials = Objects.requireNonNull(credentials);
    }

    public void activate(String cpf, String password) {
        if (cpf == null || !cpf.matches("\\d{11}")) throw new InvalidFirstAccessException();
        String hash = credentials.hash(password);
        if (!accounts.activateFirstAccess(cpf, hash)) throw new InvalidFirstAccessException();
    }
}
