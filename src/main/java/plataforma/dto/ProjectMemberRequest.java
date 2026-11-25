package plataforma.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import plataforma.model.InvitationStatus;
import plataforma.model.MemberRole;

public record ProjectMemberRequest(
        @NotNull UUID projectId,
        @NotNull UUID userId,
        @NotNull MemberRole memberRole,
        @NotNull InvitationStatus status) {
}
