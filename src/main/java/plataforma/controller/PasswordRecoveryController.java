package plataforma.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import plataforma.auth.passwordrecovery.PasswordRecoveryRequestService;
import plataforma.auth.passwordrecovery.PasswordResetService;

/** HTTP surface kept conditional until the account and e-mail adapters are supplied. */
@RestController
@ConditionalOnBean({PasswordRecoveryRequestService.class, PasswordResetService.class})
@RequestMapping("/api/v1/auth")
public class PasswordRecoveryController {

    private final PasswordRecoveryRequestService requestService;
    private final PasswordResetService resetService;

    public PasswordRecoveryController(
            PasswordRecoveryRequestService requestService,
            PasswordResetService resetService) {
        this.requestService = requestService;
        this.resetService = resetService;
    }

    @PostMapping("/password-recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        requestService.request(request.email());
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        resetService.reset(request.token(), request.newPassword());
    }

    public record PasswordRecoveryRequest(@NotBlank @Email String email) {
    }

    public record PasswordResetRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }
}
