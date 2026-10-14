package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.ProjectMemberRequest;
import plataforma.dto.ProjectMemberResponse;
import plataforma.model.ProjectMember;

@Component
public class ProjectMemberMapper {

    public ProjectMemberResponse toResponse(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getProject().getId(),
                member.getUser().getId(),
                member.getMemberRole(),
                member.getStatus(),
                member.getInvitedAt());
    }

    /** Copia os campos simples. Projeto e usuário são resolvidos no service. */
    public void copyToEntity(ProjectMemberRequest request, ProjectMember member) {
        member.setMemberRole(request.memberRole());
        member.setStatus(request.status());
    }
}
