package plataforma.auth;

import java.util.UUID;

public record AuthenticatedAccount(UUID id, String passwordHash, String status, String role) {
    @Override
    public String toString() {
        return "AuthenticatedAccount[id=" + id + ", status=" + status + ", role=" + role + "]";
    }
}
