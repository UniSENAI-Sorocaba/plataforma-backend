package br.senai.plataformadigital.auth.passwordrecovery;

import java.util.Map;

import br.senai.plataformadigital.auth.CredentialService;
import br.senai.plataformadigital.auth.UserAccountRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP contract for neutral recovery requests and one-use password reset. */
@RestController
@RequestMapping("/api/v1/auth/password-recovery")
@ConditionalOnBean({PasswordRecoveryTokenService.class, UserAccountRepository.class, CredentialService.class})
public class PasswordRecoveryController {
    private final PasswordRecoveryTokenService tokens;
    private final UserAccountRepository accounts;
    private final CredentialService credentials;
    private final PasswordRecoveryRequestService requests;

    public PasswordRecoveryController(PasswordRecoveryTokenService tokens, UserAccountRepository accounts,
            CredentialService credentials, org.springframework.beans.factory.ObjectProvider<PasswordRecoveryRequestService> requests) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.credentials = credentials;
        this.requests = requests.getIfAvailable();
    }

    @PostMapping("/request")
    ResponseEntity<Void> request(@Valid @RequestBody RecoveryRequest request) {
        if (requests != null) requests.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest request) {
        PasswordRecoveryTokenConsumption consumption = tokens.consume(request.token());
        if (!consumption.isConsumed() || !accounts.updatePasswordByEmail(consumption.subject(), credentials.hash(request.password()))) {
            throw new InvalidPasswordRecoveryException();
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidPasswordRecoveryException.class)
    ResponseEntity<Map<String, Object>> invalid() {
        return ResponseEntity.badRequest().body(Map.of(
                "type", "about:blank", "title", "Bad Request", "status", 400,
                "detail", "Não foi possível redefinir a senha."));
    }

    public record RecoveryRequest(@NotBlank @Email String email) {}

    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 6, max = 128) String password) {}

    public static final class InvalidPasswordRecoveryException extends RuntimeException {
        public InvalidPasswordRecoveryException() {
            super("Não foi possível redefinir a senha.");
        }
    }
}
