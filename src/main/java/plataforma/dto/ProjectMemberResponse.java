package plataforma.dto;

import java.time.Instant;
import java.util.UUID;
import plataforma.model.InvitationStatus;
import plataforma.model.MemberRole;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        MemberRole memberRole,
        InvitationStatus status,
        Instant invitedAt) {
}
