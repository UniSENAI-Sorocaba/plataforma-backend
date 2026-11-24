package plataforma.repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import plataforma.model.InvitationStatus;
import plataforma.model.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByUserAndStatus(
            UUID valorUser, 
            InvitationStatus valorStatus
    );
}
