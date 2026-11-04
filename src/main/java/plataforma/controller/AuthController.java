package plataforma.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import plataforma.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String newToken = authService.rotateRefreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("refreshToken", newToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}