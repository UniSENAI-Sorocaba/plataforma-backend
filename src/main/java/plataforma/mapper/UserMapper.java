package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.UserRequest;
import plataforma.dto.UserResponse;
import plataforma.model.UserAccount;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public void copyToEntity(UserRequest request, UserAccount user) {
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setActive(request.active());
    }
}
