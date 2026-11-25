package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.Role;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
