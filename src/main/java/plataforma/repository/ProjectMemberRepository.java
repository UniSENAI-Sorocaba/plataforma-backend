package plataforma.repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import plataforma.model.InvitationStatus;
import plataforma.model.Project;
import plataforma.model.ProjectMember;
import plataforma.model.ProjectStatus;
import plataforma.model.UserAccount;


public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByUserAndStatus(
            UserAccount objetoUser, 
            InvitationStatus valorStatus
    );

    Page<ProjectMember> findByStatusNot(InvitationStatus status, Pageable pageable);
}
