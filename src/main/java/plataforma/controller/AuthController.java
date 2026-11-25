package plataforma.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plataforma.auth.AuthenticationService;
import plataforma.auth.LoginRequest;
import plataforma.auth.LoginResponse;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request, servletRequest.getRemoteAddr()));
    }
}
